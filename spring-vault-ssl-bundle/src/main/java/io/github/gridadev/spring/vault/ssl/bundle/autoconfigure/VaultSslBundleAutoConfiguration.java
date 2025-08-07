package io.github.gridadev.spring.vault.ssl.bundle.autoconfigure;

import io.github.gridadev.spring.vault.ssl.bundle.VaultSslBundleRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.autoconfigure.ssl.SslProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.core.VaultTemplate;

/**
 * Auto-configuration for Vault SSL Bundle support.
 * Enables SSL bundles to be loaded from HashiCorp Vault.
 */
@AutoConfiguration
public class VaultSslBundleAutoConfiguration {

    @Bean
    public SslBundleRegistrar serverPropertiesSslBundleRegistrar(
            SslProperties properties, VaultTemplate vaultTemplate) {
        return new VaultSslBundleRegistrar(properties, vaultTemplate);
    }
}
