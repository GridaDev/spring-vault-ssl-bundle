package io.github.gridadev.spring.vault.ssl.bundle;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

/**
 * Registry for managing SSL bundles loaded from HashiCorp Vault.
 */
public class VaultSslBundleRegistry extends DefaultSslBundleRegistry {

    private static final Logger logger = LoggerFactory.getLogger(VaultSslBundleRegistry.class);

    private final VaultTemplate vaultTemplate;

    public VaultSslBundleRegistry(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    @Override
    public SslBundle getBundle(String name) throws NoSuchSslBundleException {

        if (name.startsWith("vault:")) {

            var bundleFromVault = loadBundleFromVault(name);

            this.registerBundle(name, bundleFromVault);
        }

        return super.getBundle(name);
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
}
