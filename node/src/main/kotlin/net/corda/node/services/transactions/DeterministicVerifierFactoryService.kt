package net.corda.node.services.transactions

import net.corda.core.internal.BasicVerifier
import net.corda.core.internal.Verifier
import net.corda.core.serialization.ConstructorForDeserialization
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.DeprecatedConstructorForDeserialization
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.LedgerTransaction
import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.Whitelist
import net.corda.djvm.execution.ExecutionProfile
import net.corda.djvm.source.ApiSource
import net.corda.djvm.source.UserPathSource
import net.corda.djvm.source.UserSource
import net.corda.node.internal.djvm.DeterministicVerifier
import java.net.URL
import java.net.URLClassLoader
import java.util.function.UnaryOperator

interface VerifierFactoryService : UnaryOperator<LedgerTransaction>, AutoCloseable

class DeterministicVerifierFactoryService(
    private val bootstrapSource: ApiSource,
    private val cordaSource: UserSource
) : SingletonSerializeAsToken(), VerifierFactoryService {
    private val baseSandboxConfiguration: SandboxConfiguration

    init {
        val baseAnalysisConfiguration = AnalysisConfiguration.createRoot(
            userSource = cordaSource,
            whitelist = Whitelist.MINIMAL,
            visibleAnnotations = setOf(
                CordaSerializable::class.java,
                ConstructorForDeserialization::class.java,
                DeprecatedConstructorForDeserialization::class.java
            ),
            bootstrapSource = bootstrapSource
        )

        baseSandboxConfiguration = SandboxConfiguration.createFor(
            analysisConfiguration = baseAnalysisConfiguration,
            profile = NODE_PROFILE,
            enableTracing = true
        )
    }

    /**
     * Generate sandbox classes for every Corda jar with META-INF/DJVM-preload.
     */
    fun generateSandbox(): DeterministicVerifierFactoryService {
        baseSandboxConfiguration.preload()
        return this
    }

    override fun apply(ledgerTransaction: LedgerTransaction): LedgerTransaction {
        // Specialise the LedgerTransaction here so that
        // contracts are verified inside the DJVM!
        return ledgerTransaction.specialise(::specialise)
    }

    private fun specialise(ltx: LedgerTransaction, classLoader: ClassLoader): Verifier {
        return (classLoader as? URLClassLoader)?.run {
            DeterministicVerifier(ltx, classLoader, createSandbox(classLoader.urLs))
        } ?: BasicVerifier(ltx, classLoader)
    }

    private fun createSandbox(userSource: Array<URL>): SandboxConfiguration {
        return baseSandboxConfiguration.createChild(UserPathSource(userSource))
    }

    override fun close() {
        bootstrapSource.use {
            cordaSource.close()
        }
    }

    private companion object {
        private val NODE_PROFILE = ExecutionProfile(
            allocationCostThreshold = 1024 * 1024 * 1024,
            invocationCostThreshold = 100_000_000,
            jumpCostThreshold = 500_000_000,
            throwCostThreshold = 1_000_000
        )
    }
}

class BasicVerifierFactoryService : VerifierFactoryService {
    override fun apply(ledgerTransaction: LedgerTransaction)= ledgerTransaction
    override fun close() {}
}
