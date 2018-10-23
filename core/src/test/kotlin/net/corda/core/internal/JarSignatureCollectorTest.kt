package net.corda.core.internal

import net.corda.core.JarSignatureTestUtils.createJar
import net.corda.core.JarSignatureTestUtils.generateKey
import net.corda.core.JarSignatureTestUtils.getJarSigners
import net.corda.core.JarSignatureTestUtils.signJar
import net.corda.core.JarSignatureTestUtils.updateJar
import net.corda.core.identity.Party
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.core.CHARLIE_NAME
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JarSignatureCollectorTest {
    companion object {
        private val dir = Files.createTempDirectory(JarSignatureCollectorTest::class.simpleName)

        private const val FILENAME = "attachment.jar"
        private const val ALICE = "alice"
        private const val ALICE_PASS = "alicepass"
        private const val BOB = "bob"
        private const val BOB_PASS = "bobpass"
        private const val CHARLIE = "Charlie"
        private const val CHARLIE_PASS = "charliepass"

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            dir.generateKey(ALICE, ALICE_PASS, ALICE_NAME)
            dir.generateKey(BOB, BOB_PASS, BOB_NAME)
            dir.generateKey(CHARLIE, CHARLIE_PASS, CHARLIE_NAME, "EC")

            (dir / "_signable1").writeLines(listOf("signable1"))
            (dir / "_signable2").writeLines(listOf("signable2"))
            (dir / "_signable3").writeLines(listOf("signable3"))
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            dir.deleteRecursively()
        }
    }

    private val List<Party>.keys get() = map { it.owningKey }

    @After
    fun tearDown() {
        dir.list {
            it.filter { !it.fileName.toString().startsWith("_") }.forEach(Path::deleteRecursively)
        }
        assertThat(dir.list()).hasSize(5)
    }

    @Test
    fun `empty jar has no signers`() {
        (dir / "META-INF").createDirectory() // At least one arg is required, and jar cvf conveniently ignores this.
        dir.createJar(FILENAME, "META-INF")
        assertEquals(emptyList(), dir.getJarSigners(FILENAME))

        signAsAlice()
        assertEquals(emptyList(), dir.getJarSigners(FILENAME)) // There needs to have been a file for ALICE to sign.
    }

    @Test
    fun `unsigned jar has no signers`() {
        dir.createJar(FILENAME, "_signable1")
        assertEquals(emptyList(), dir.getJarSigners(FILENAME))

        dir.updateJar(FILENAME, "_signable2")
        assertEquals(emptyList(), dir.getJarSigners(FILENAME))
    }

    @Test
    fun `one signer`() {
        dir.createJar(FILENAME, "_signable1", "_signable2")
        val key = signAsAlice()
        assertEquals(listOf(key), dir.getJarSigners(FILENAME))

        (dir / "my-dir").createDirectory()
        dir.updateJar(FILENAME, "my-dir")
        assertEquals(listOf(key), dir.getJarSigners(FILENAME)) // Unsigned directory is irrelevant.
    }

    @Test
    fun `two signers`() {
        dir.createJar(FILENAME, "_signable1", "_signable2")
        val key1 = signAsAlice()
        val key2 = signAsBob()

        assertEquals(setOf(key1, key2), dir.getJarSigners(FILENAME).toSet())
    }

    @Test
    fun `all files must be signed by the same set of signers`() {
        dir.createJar(FILENAME, "_signable1")
        val key1 = signAsAlice()
        assertEquals(listOf(key1), dir.getJarSigners(FILENAME))

        dir.updateJar(FILENAME, "_signable2")
        signAsBob()
        assertFailsWith<InvalidJarSignersException>(
                """
            Mismatch between signers [O=Alice Corp, L=Madrid, C=ES, O=Bob Plc, L=Rome, C=IT] for file _signable1
            and signers [O=Bob Plc, L=Rome, C=IT] for file _signable2.
            See https://docs.corda.net/design/data-model-upgrades/signature-constraints.html for details of the
            constraints applied to attachment signatures.
            """.trimIndent().replace('\n', ' ')
        ) { dir.getJarSigners(FILENAME) }
    }

    @Test
    fun `bad signature is caught even if the party would not qualify as a signer`() {
        (dir / "volatile").writeLines(listOf("volatile"))
        dir.createJar(FILENAME, "volatile")
        val key1 = signAsAlice()
        assertEquals(listOf(key1), dir.getJarSigners(FILENAME))

        (dir / "volatile").writeLines(listOf("garbage"))
        dir.updateJar(FILENAME, "volatile", "_signable1") // ALICE's signature on volatile is now bad.
        signAsBob()
        // The JDK doesn't care that BOB has correctly signed the whole thing, it won't let us process the entry with ALICE's bad signature:
        assertFailsWith<SecurityException> { dir.getJarSigners(FILENAME) }
    }

    // Signing using EC algorithm produced JAR File spec incompatible signature block (META-INF/*.EC) which is anyway accepted by jarsiner, see [JarSignatureCollector]
    @Test
    fun `one signer with EC sign algorithm`() {
        createJar("_signable1", "_signable2")
        signJar(CHARLIE, CHARLIE_PASS)
        assertEquals(listOf(CHARLIE_NAME), getJarSigners().names) // We only reused CHARLIE's distinguished name, so the keys will be different.

        (dir / "my-dir").createDirectory()
        updateJar("my-dir")
        assertEquals(listOf(CHARLIE_NAME), getJarSigners().names) // Unsigned directory is irrelevant.
    }

    private fun signAsAlice() = dir.signJar(FILENAME, ALICE, ALICE_PASS)
    private fun signAsBob() = dir.signJar(FILENAME, BOB, BOB_PASS)
}
