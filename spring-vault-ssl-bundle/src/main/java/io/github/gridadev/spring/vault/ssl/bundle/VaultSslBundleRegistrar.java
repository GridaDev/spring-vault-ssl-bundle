package io.github.gridadev.spring.vault.ssl.bundle;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.autoconfigure.ssl.SslProperties;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

/**
 * SSL Bundle Registrar that integrates with HashiCorp Vault to dynamically load SSL certificates.
 * Supports both KV v1 and KV v2 secret engines with simplified configuration.
 *
 * <p>Usage examples:
 * <pre>
 * # Simple approach - specify vault path once per bundle
 * spring:
 *   ssl:
 *     bundle:
 *       pem:
 *         server-a:
 *           keystore:
 *             certificate: "vault:secret/data/ssl-certs/server-a"
 *
 * # Advanced approach - specify individual field paths
 * spring:
 *   ssl:
 *     bundle:
 *       pem:
 *         server-b:
 *           keystore:
 *             certificate: "vault:secret/data/ssl-certs/server-b:certificate"
 *             private-key: "vault:secret/data/ssl-certs/server-b:private-key"
 *           truststore:
 *             certificate: "vault:secret/data/ssl-certs/server-b:ca-certificate"
 * </pre>
 */
public class VaultSslBundleRegistrar implements SslBundleRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(VaultSslBundleRegistrar.class);

    private static final String DEFAULT_VAULT_PREFIX = "vault:";
    private static final String CERTIFICATE_KEY = "certificate";
    private static final String PRIVATE_KEY_KEY = "private-key";
    private static final String CA_CERTIFICATE_KEY = "ca-certificate";

    private final SslProperties.Bundles properties;
    private final VaultTemplate vaultTemplate;
    private final String vaultPrefix;

    public VaultSslBundleRegistrar(SslProperties properties, VaultTemplate vaultTemplate) {
        this(properties, vaultTemplate, DEFAULT_VAULT_PREFIX);
    }

    public VaultSslBundleRegistrar(SslProperties properties, VaultTemplate vaultTemplate, String vaultPrefix) {
        this.properties = properties.getBundle();
        this.vaultTemplate = vaultTemplate;
        this.vaultPrefix = StringUtils.hasText(vaultPrefix) ? vaultPrefix : DEFAULT_VAULT_PREFIX;
    }

    @Override
    public void registerBundles(SslBundleRegistry registry) {
        if (properties.getPem() == null) {
            logger.debug("No PEM bundles configured, skipping Vault SSL bundle registration");
            return;
        }

        properties.getPem().forEach((bundleName, bundleProperties) -> {
            logger.debug("Processing SSL bundle: {}", bundleName);

            try {
                processBundleCertificates(bundleName, bundleProperties);
            } catch (Exception e) {
                logger.error("Failed to process SSL bundle '{}' from Vault: {}", bundleName, e.getMessage(), e);
                throw new VaultSslBundleException("Failed to load SSL bundle '" + bundleName + "' from Vault", e);
            }
        });
    }

    private void processBundleCertificates(String bundleName, PemSslBundleProperties bundleProperties) {
        // Create a cache to avoid duplicate Vault calls within the same bundle
        Map<String, Map<String, Object>> vaultDataCache = new HashMap<>();

        if (bundleProperties.getKeystore() != null) {
            processKeystoreCertificate(bundleName, bundleProperties, vaultDataCache);
        }

        if (bundleProperties.getTruststore() != null) {
            processTruststoreCertificate(bundleName, bundleProperties, vaultDataCache);
        }
    }

    private void processKeystoreCertificate(
            String bundleName,
            PemSslBundleProperties bundleProperties,
            Map<String, Map<String, Object>> vaultDataCache) {
        String certificatePath = bundleProperties.getKeystore().getCertificate();
        String privateKeyPath = bundleProperties.getKeystore().getPrivateKey();

        if (isVaultPath(certificatePath)) {
            logger.info("Loading keystore certificates for bundle '{}' from Vault", bundleName);

            VaultPathInfo certPathInfo = parseVaultPath(certificatePath, CERTIFICATE_KEY);
            VaultPathInfo keyPathInfo = isVaultPath(privateKeyPath)
                    ? parseVaultPath(privateKeyPath, PRIVATE_KEY_KEY)
                    : new VaultPathInfo(certPathInfo.path, PRIVATE_KEY_KEY); // Default to same path, different field

            // Load certificate data (using cache)
            Map<String, Object> certificateData =
                    loadCertificateDataFromVaultWithCache(certPathInfo.path, vaultDataCache);

            // Set certificate
            String certificate = extractFieldValue(certificateData, certPathInfo.field, bundleName, "certificate");
            if (certificate != null) {
                bundleProperties.getKeystore().setCertificate(certificate);
            }

            // Set private key (load from same or different path, using cache)
            Map<String, Object> keyData = loadCertificateDataFromVaultWithCache(keyPathInfo.path, vaultDataCache);
            String privateKey = extractFieldValue(keyData, keyPathInfo.field, bundleName, "private key");

            if (privateKey != null) {
                bundleProperties.getKeystore().setPrivateKey(privateKey);
            }
        }
    }

    private void processTruststoreCertificate(
            String bundleName,
            PemSslBundleProperties bundleProperties,
            Map<String, Map<String, Object>> vaultDataCache) {
        String certificatePath = bundleProperties.getTruststore().getCertificate();

        if (isVaultPath(certificatePath)) {
            logger.info("Loading truststore certificate for bundle '{}' from Vault", bundleName);

            VaultPathInfo pathInfo = parseVaultPath(certificatePath, CA_CERTIFICATE_KEY);
            Map<String, Object> certificateData = loadCertificateDataFromVaultWithCache(pathInfo.path, vaultDataCache);

            // Try CA certificate first, then fall back to regular certificate
            String caCertificate = extractFieldValue(certificateData, pathInfo.field, bundleName, "CA certificate");
            if (caCertificate == null && CA_CERTIFICATE_KEY.equals(pathInfo.field)) {
                caCertificate = extractFieldValue(
                        certificateData, CERTIFICATE_KEY, bundleName, "certificate (fallback for CA)");
            }

            if (caCertificate != null) {
                bundleProperties.getTruststore().setCertificate(caCertificate);
            }
        }
    }

    private String extractFieldValue(
            Map<String, Object> data, String fieldName, String bundleName, String fieldDescription) {
        Object value = data.get(fieldName);
        if (value instanceof String) {
            return (String) value;
        } else if (value == null) {
            logger.warn(
                    "Field '{}' ({}) not found in Vault data for bundle '{}'", fieldName, fieldDescription, bundleName);
        } else {
            logger.warn(
                    "Field '{}' ({}) is not a string in Vault data for bundle '{}': {}",
                    fieldName,
                    fieldDescription,
                    bundleName,
                    value.getClass().getSimpleName());
        }
        return null;
    }

    private Map<String, Object> loadCertificateDataFromVaultWithCache(
            String vaultPath, Map<String, Map<String, Object>> cache) {
        // Check cache first
        if (cache.containsKey(vaultPath)) {
            logger.debug("Using cached data for Vault path: {}", vaultPath);
            return cache.get(vaultPath);
        }

        // Load from Vault and cache the result
        logger.debug("Loading data from Vault path: {} (not in cache)", vaultPath);
        Map<String, Object> data = loadCertificateDataFromVault(vaultPath);
        cache.put(vaultPath, data);

        return data;
    }

    private Map<String, Object> loadCertificateDataFromVault(String vaultPath) {
        logger.debug("Reading certificate data from Vault path: {}", vaultPath);

        VaultResponse response = vaultTemplate.read(vaultPath);
        if (response == null) {
            throw new VaultSslBundleException("No data found at Vault path: " + vaultPath);
        }

        Map<String, Object> responseData = response.getData();
        if (responseData == null || responseData.isEmpty()) {
            throw new VaultSslBundleException("Empty response data from Vault path: " + vaultPath);
        }

        // Handle both KV v1 and KV v2 secret engines
        return extractCertificateData(responseData, vaultPath);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractCertificateData(Map<String, Object> responseData, String vaultPath) {
        // Check if this is KV v2 (has nested 'data' field)
        if (responseData.containsKey("data") && responseData.get("data") instanceof Map) {
            logger.debug("Detected KV v2 secret engine format for path: {}", vaultPath);
            return (Map<String, Object>) responseData.get("data");
        }

        // Assume KV v1 format (flat structure)
        logger.debug("Using KV v1 secret engine format for path: {}", vaultPath);
        return responseData;
    }

    private VaultPathInfo parseVaultPath(String fullPath, String defaultFieldName) {
        if (!isVaultPath(fullPath)) {
            throw new IllegalArgumentException("Path does not start with vault prefix: " + fullPath);
        }

        String pathAfterPrefix = fullPath.substring(vaultPrefix.length());

        if (!StringUtils.hasText(pathAfterPrefix)) {
            throw new IllegalArgumentException("Empty vault path after prefix: " + fullPath);
        }

        // Check if path contains field specification (path:field)
        int colonIndex = pathAfterPrefix.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < pathAfterPrefix.length() - 1) {
            String vaultPath = pathAfterPrefix.substring(0, colonIndex);
            String fieldName = pathAfterPrefix.substring(colonIndex + 1);
            return new VaultPathInfo(vaultPath, fieldName);
        } else {
            // No field specified, use default field names based on context
            return new VaultPathInfo(pathAfterPrefix, defaultFieldName);
        }
    }

    private boolean isVaultPath(String path) {
        return StringUtils.hasText(path) && path.startsWith(vaultPrefix);
    }

    /**
     * Holds vault path and optional field name information.
     */
    private static class VaultPathInfo {
        final String path;
        final String field;

        VaultPathInfo(String path, String field) {
            this.path = path;
            this.field = field;
        }
    }

    /**
     * Exception thrown when SSL bundle loading from Vault fails.
     */
    public static class VaultSslBundleException extends RuntimeException {
        public VaultSslBundleException(String message) {
            super(message);
        }

        public VaultSslBundleException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
