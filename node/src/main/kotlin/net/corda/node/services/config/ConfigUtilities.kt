package net.corda.node.services.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import net.corda.cliutils.CordaSystemUtils
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.createDirectories
import net.corda.core.internal.div
import net.corda.core.internal.exists
import net.corda.nodeapi.internal.DEV_CA_KEY_STORE_PASS
import net.corda.nodeapi.internal.config.FileBasedCertificateStoreSupplier
import net.corda.nodeapi.internal.config.MutualSslConfiguration
import net.corda.nodeapi.internal.config.toProperties
import net.corda.nodeapi.internal.crypto.X509KeyStore
import net.corda.nodeapi.internal.cryptoservice.CryptoService
import net.corda.nodeapi.internal.cryptoservice.bouncycastle.BCCryptoService
import net.corda.nodeapi.internal.installDevNodeCaCertPath
import net.corda.nodeapi.internal.loadDevCaTrustStore
import net.corda.nodeapi.internal.registerDevP2pCertificates
import org.slf4j.LoggerFactory
import java.nio.file.Path

fun configOf(vararg pairs: Pair<String, Any?>): Config = ConfigFactory.parseMap(mapOf(*pairs))
operator fun Config.plus(overrides: Map<String, Any?>): Config = ConfigFactory.parseMap(overrides).withFallback(this)

object ConfigHelper {

    private const val CORDA_PROPERTY_PREFIX = "corda."

    private val log = LoggerFactory.getLogger(javaClass)
    fun loadConfig(baseDirectory: Path,
                   configFile: Path = baseDirectory / "node.conf",
                   allowMissingConfig: Boolean = false,
                   configOverrides: Config = ConfigFactory.empty()): Config {
        val parseOptions = ConfigParseOptions.defaults()
        val defaultConfig = ConfigFactory.parseResources("reference.conf", parseOptions.setAllowMissing(false))
        val appConfig = ConfigFactory.parseFile(configFile.toFile(), parseOptions.setAllowMissing(allowMissingConfig))

        // Detect the underlying OS. If mac or windows non-server then we assume we're running in devMode. Unless specified otherwise.
        val smartDevMode = CordaSystemUtils.isOsMac() || (CordaSystemUtils.isOsWindows() && !CordaSystemUtils.getOsName().toLowerCase().contains("server"))
        val devModeConfig = ConfigFactory.parseMap(mapOf("devMode" to smartDevMode))

        val systemOverrides = ConfigFactory.systemProperties().cordaEntriesOnly()
        val environmentOverrides = ConfigFactory.systemEnvironment().cordaEntriesOnly()
        val finalConfig = configOf(
                // Add substitution values here
                "baseDirectory" to baseDirectory.toString())
                .withFallback(configOverrides)
                .withFallback(systemOverrides)
                .withFallback(environmentOverrides)
                .withFallback(appConfig)
                .withFallback(devModeConfig) // this needs to be after the appConfig, so it doesn't override the configured devMode
                .withFallback(defaultConfig)
                .resolve()

        val entrySet = finalConfig.entrySet().filter { entry ->
            // System properties can reasonably be expected to contain '.'. However, only
            // entries that match this pattern are allowed to contain a '"' character:
            //           systemProperties."text-without-any-quotes-in"
            with(entry.key) { contains("\"") && !matches("^systemProperties\\.\"[^\"]++\"\$".toRegex()) }
        }
        for (key in entrySet) {
            log.error("Config files should not contain \" in property names. Please fix: $key")
        }

        return finalConfig
    }

    private fun Config.cordaEntriesOnly(): Config {
        return ConfigFactory.parseMap(toProperties()
            .filterKeys { (it as String).startsWith(CORDA_PROPERTY_PREFIX) }
            .mapKeys { (it.key as String).removePrefix(CORDA_PROPERTY_PREFIX) }
        )
    }
}

/**
 * Strictly for dev only automatically construct a server certificate/private key signed from
 * the CA certs in Node resources. Then provision KeyStores into certificates folder under node path.
 */
// TODO Move this to KeyStoreConfigHelpers.
fun NodeConfiguration.configureWithDevSSLCertificate(cryptoService: CryptoService? = null) = p2pSslOptions.configureDevKeyAndTrustStores(myLegalName, signingCertificateStore, certificatesDirectory, cryptoService)

// TODO Move this to KeyStoreConfigHelpers.
fun MutualSslConfiguration.configureDevKeyAndTrustStores(myLegalName: CordaX500Name, signingCertificateStore: FileBasedCertificateStoreSupplier, certificatesDirectory: Path, cryptoService: CryptoService? = null) {
    val specifiedTrustStore = trustStore.getOptional()

    val specifiedKeyStore = keyStore.getOptional()
    val specifiedSigningStore = signingCertificateStore.getOptional()

    if (specifiedTrustStore != null && specifiedKeyStore != null && specifiedSigningStore != null) return
    certificatesDirectory.createDirectories()

    if (specifiedTrustStore == null) {
        loadDevCaTrustStore().copyTo(trustStore.get(true))
    }

    if (specifiedKeyStore == null || specifiedSigningStore == null) {
        FileBasedCertificateStoreSupplier(keyStore.path, keyStore.storePassword, keyStore.entryPassword).get(true)
                .also { it.registerDevP2pCertificates(myLegalName) }
        when (cryptoService) {
            is BCCryptoService, null -> {
                val signingKeyStore = FileBasedCertificateStoreSupplier(signingCertificateStore.path, signingCertificateStore.storePassword, signingCertificateStore.entryPassword).get(true)
                        .also { it.installDevNodeCaCertPath(myLegalName) }

                // Move distributed service composite key (generated by IdentityGenerator.generateToDisk) to keystore if exists.
                val distributedServiceKeystore = certificatesDirectory / "distributedService.jks"
                if (distributedServiceKeystore.exists()) {
                    val serviceKeystore = X509KeyStore.fromFile(distributedServiceKeystore, DEV_CA_KEY_STORE_PASS)

                    signingKeyStore.update {
                        serviceKeystore.aliases().forEach {
                            if (serviceKeystore.internal.isKeyEntry(it)) {
                                setPrivateKey(it, serviceKeystore.getPrivateKey(it, DEV_CA_KEY_STORE_PASS), serviceKeystore.getCertificateChain(it), signingKeyStore.entryPassword)
                            } else {
                                setCertificate(it, serviceKeystore.getCertificate(it))
                            }
                        }
                    }
                }
            }
            else -> throw IllegalArgumentException("CryptoService not supported.")
        }
    }
}