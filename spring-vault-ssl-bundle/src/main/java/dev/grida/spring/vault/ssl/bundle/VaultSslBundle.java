package dev.grida.spring.vault.ssl.bundle;

import org.springframework.boot.ssl.*;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;

/**
 * SSL Bundle implementation that wraps certificates loaded from Vault.
 */
public class VaultSslBundle implements SslBundle {

    private final SslStoreBundle stores;
    private final SslBundleKey key;

    public VaultSslBundle(String certificate, String privateKey, String caCertificate) {
        this.key = SslBundleKey.of("");
        this.stores = createStoreBundle(certificate, privateKey, caCertificate);
    }

    private SslStoreBundle createStoreBundle(String certificate, String privateKey, String caCertificate) {
        PemSslStoreDetails keyStoreDetails =
                PemSslStoreDetails.forCertificate(certificate).withPrivateKey(privateKey);

        PemSslStoreDetails trustStoreDetails = null;
        if (caCertificate != null && !caCertificate.trim().isEmpty()) {
            trustStoreDetails = PemSslStoreDetails.forCertificate(caCertificate);
        }

        return new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
    }

    @Override
    public SslStoreBundle getStores() {
        return stores;
    }

    @Override
    public SslBundleKey getKey() {
        return key;
    }

    @Override
    public SslOptions getOptions() {
        return SslOptions.NONE;
    }

    @Override
    public SslManagerBundle getManagers() {
        return SslManagerBundle.from(stores, key);
    }

    @Override
    public String getProtocol() {
        return "TLS";
    }
}
