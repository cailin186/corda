package net.corda.testing;

//Why Java?! because sometimes types are useful.

import groovy.lang.Tuple2;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BucketingAllocator {
    private static final Logger LOG = LoggerFactory.getLogger(BucketingAllocator.class);
    private final List<TestsForForkContainer> forkContainers;
    private final Supplier<Tests> timedTestsProvider;
    private List<Tuple2<TestLister, Object>> sources = new ArrayList<>();


    public BucketingAllocator(Integer forkCount, Supplier<Tests> timedTestsProvider) {
        this.forkContainers = IntStream.range(0, forkCount).mapToObj(TestsForForkContainer::new).collect(Collectors.toList());
        this.timedTestsProvider = timedTestsProvider;
    }

    public void addSource(TestLister source, Object testTask) {
        sources.add(new Tuple2<>(source, testTask));
    }

    public List<String> getTestsForForkAndTestTask(Integer fork, Object testTask) {
        return forkContainers.get(fork).getTestsForTask(testTask);
    }

    @TaskAction
    public void generateTestPlan() {
        Tests allTestsFromFile = timedTestsProvider.get();
        List<Tuple2<String, Object>> allDiscoveredTests = getTestsOnClasspathOfTestingTasks();
        List<TestBucket> matchedTests = matchClasspathTestsToFile(allTestsFromFile, allDiscoveredTests);

        //use greedy algo - for each testbucket find the currently smallest container and add to it
        allocateTestsToForks(matchedTests);
        forkContainers.forEach(TestsForForkContainer::freeze);

        printSummary();
    }

    private void printSummary() {
        forkContainers.forEach(container -> {
            System.out.println("####### TEST PLAN SUMMARY ( " + container.forkIdx + " ) #######");
            System.out.println("Duration (secs): " + TimeUnit.NANOSECONDS.toSeconds(container.getCurrentDuration()));
            System.out.println("Number of tests: " + container.testsForFork.stream().mapToInt(b -> b.foundTests.size()).sum());
            System.out.println("Tests to Run: ");
            container.testsForFork.forEach(tb -> {
                System.out.println(tb.testName);
                tb.foundTests.forEach(ft -> System.out.println("\t" + ft.getFirst() + ", " + ft.getSecond()));
            });
        });
    }

    private void allocateTestsToForks(@NotNull List<TestBucket> matchedTests) {
        matchedTests.forEach(matchedTestBucket -> {
            TestsForForkContainer smallestContainer = Collections.min(forkContainers, Comparator.comparing(TestsForForkContainer::getCurrentDuration));
            smallestContainer.addBucket(matchedTestBucket);
        });
    }

    private List<TestBucket> matchClasspathTestsToFile(@NotNull final Tests tests,
                                                       @NotNull final List<Tuple2<String, Object>> allDiscoveredTests) {
        return allDiscoveredTests.stream().map(tuple -> {
            final String testName = tuple.getFirst();
            final Object task = tuple.getSecond();

            // If the gradle task is distributing by class rather than method, then 'testName' will be the className
            // and not className.testName
            final List<Tuple2<String, Long>> matchingTests = tests.startsWith(testName);

            // 'matchingTests' may be empty if we've never run those tests before, so we give it a default
            // size so that it is fairly allocated to a bucket.  The best we can do at the moment, is
            // to try and use the individual mean test duration.  If we pass the 'distribute' type
            // in here, we could use mean duration for classes if required.
            return new TestBucket(task, testName, matchingTests, tests.getMeanDurationForTests());
        }).sorted(Comparator.comparing(TestBucket::getDuration).reversed()).collect(Collectors.toList());
    }

    private List<Tuple2<String, Object>> getTestsOnClasspathOfTestingTasks() {
        return sources.stream().map(source -> {
            TestLister lister = source.getFirst();
            Object testTask = source.getSecond();
            return lister.getAllTestsDiscovered().stream().map(test -> new Tuple2<>(test, testTask)).collect(Collectors.toList());
        }).flatMap(Collection::stream).sorted(Comparator.comparing(Tuple2::getFirst)).collect(Collectors.toList());
    }

    public static class TestBucket {
        final Object testTask;
        final String testName;
        final List<Tuple2<String, Long>> foundTests;
        final long durationNanos;

        public TestBucket(@NotNull final Object testTask,
                          @NotNull final String testName,
                          @NotNull final List<Tuple2<String, Long>> foundTests,
                          long defaultBucketDuration) {
            this.testTask = testTask;
            this.testName = testName;
            this.foundTests = foundTests;
            durationNanos = Math.max(
                    foundTests.stream().mapToLong(tp -> Math.max(tp.getSecond(), 1)).sum(),
                    defaultBucketDuration);
        }

        public long getDuration() {
            return durationNanos;
        }

        @Override
        public String toString() {
            return "TestBucket{" +
                    "testTask=" + testTask +
                    ", nameWithAsterix='" + testName + '\'' +
                    ", foundTests=" + foundTests +
                    ", durationNanos=" + durationNanos +
                    '}';
        }
    }

    public static class TestsForForkContainer {
        private final Integer forkIdx;
        private final List<TestBucket> testsForFork = Collections.synchronizedList(new ArrayList<>());
        private final Map<Object, List<TestBucket>> frozenTests = new HashMap<>();
        private long runningDuration = 0L;

        public TestsForForkContainer(Integer forkIdx) {
            this.forkIdx = forkIdx;
        }

        public void addBucket(TestBucket tb) {
            this.testsForFork.add(tb);
            this.runningDuration = runningDuration + tb.durationNanos;
        }

        public Long getCurrentDuration() {
            return runningDuration;
        }

        public void freeze() {
            testsForFork.forEach(tb -> {
                frozenTests.computeIfAbsent(tb.testTask, i -> new ArrayList<>()).add(tb);
            });
        }

        public List<String> getTestsForTask(Object task) {
            return frozenTests.getOrDefault(task, Collections.emptyList()).stream().map(it -> it.testName).collect(Collectors.toList());
        }

        public List<TestBucket> getBucketsForFork() {
            return new ArrayList<>(testsForFork);
        }

        @Override
        public String toString() {
            return "TestsForForkContainer{" +
                    "runningDuration=" + runningDuration +
                    ", forkIdx=" + forkIdx +
                    ", testsForFork=" + testsForFork +
                    ", frozenTests=" + frozenTests +
                    '}';
        }
    }
}
