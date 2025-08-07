package io.github.gridadev.spring.vault.ssl.bundle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ssl.*;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

/**
 * Registry for managing SSL bundles loaded from HashiCorp Vault.
 */
public class VaultSslBundleRegistry implements SslBundleRegistry, SslBundles {

    private static final Logger logger = LoggerFactory.getLogger(VaultSslBundleRegistry.class);

    private final VaultTemplate vaultTemplate;
    private final Map<String, SslBundle> bundles = new ConcurrentHashMap<>();

    public VaultSslBundleRegistry(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    @Override
    public void registerBundle(String name, SslBundle bundle) {

        logger.debug("RegisterBundle: {}", name);
    }

    @Override
    public void updateBundle(String name, SslBundle updatedBundle) throws NoSuchSslBundleException {
        bundles.put(name, updatedBundle);
    }

    private SslBundle loadBundleFromVault(String bundleName) {
        try {

            var vaultPath = bundleName.substring(6);

            logger.debug("Loading SSL bundle from Vault path: {}", vaultPath);

            VaultResponse response = vaultTemplate.read(vaultPath);
            if (response == null || response.getData() == null) {
                throw new RuntimeException("No SSL certificate data found at Vault path: " + vaultPath);
            }

            Map<String, Object> data = response.getData();

            // The actual certificate data is nested under "data" key for KV v2
            @SuppressWarnings("unchecked")
            Map<String, Object> certificateData = (Map<String, Object>) data.get("data");

            String certificate = (String) certificateData.get("certificate");
            String privateKey = (String) certificateData.get("private-key");
            String caCertificate = (String) certificateData.get("ca-certificate");

            if (certificate == null || privateKey == null) {
                throw new RuntimeException("Missing required certificate or private_key in Vault data");
            }

            return new VaultSslBundle(certificate, privateKey, caCertificate);

        } catch (Exception e) {
            logger.error("Failed to load SSL bundle from Vault: {}", bundleName, e);
            throw new RuntimeException("Failed to load SSL bundle: " + bundleName, e);
        }
    }

    @Override
    public SslBundle getBundle(String name) throws NoSuchSslBundleException {

        logger.debug("Requesting SSL bundle: {}", name);

        return bundles.computeIfAbsent(name, this::loadBundleFromVault);
    }

    @Override
    public void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler)
            throws NoSuchSslBundleException {}
}
