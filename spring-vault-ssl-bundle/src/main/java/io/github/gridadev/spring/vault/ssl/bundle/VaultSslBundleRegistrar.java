package io.github.gridadev.spring.vault.ssl.bundle;

import java.util.Map;
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.autoconfigure.ssl.SslProperties;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

public class VaultSslBundleRegistrar implements SslBundleRegistrar {

    private final SslProperties.Bundles properties;

    private final VaultTemplate vaultTemplate;

    public VaultSslBundleRegistrar(SslProperties properties, VaultTemplate vaultTemplate) {
        this.properties = properties.getBundle();
        this.vaultTemplate = vaultTemplate;
    }

    @Override
    public void registerBundles(SslBundleRegistry registry) {

        properties.getPem().forEach((bundleName, bundleProperties) -> {
            System.out.println("Processing bundle: " + bundleName);

            String certificatePath = bundleProperties.getKeystore().getCertificate();

            if (certificatePath.startsWith("vault:")) {

                var vaultPath = certificatePath.split(":")[1];

                VaultResponse response = vaultTemplate.read(vaultPath);
                Map<String, Object> data = response.getData();
                @SuppressWarnings("unchecked")
                Map<String, Object> certificateData = (Map<String, Object>) data.get("data");

                String certificate = (String) certificateData.get("certificate");
                bundleProperties.getKeystore().setCertificate(certificate);

                String privateKey = (String) certificateData.get("private-key");
                bundleProperties.getKeystore().setPrivateKey(privateKey);

                String caCertificate = (String) certificateData.get("ca-certificate");
                bundleProperties.getTruststore().setCertificate(caCertificate);
            }
        });
    }
}
