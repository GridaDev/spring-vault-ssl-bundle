package io.github.gridadev.spring.vault.ssl.bundle.autoconfigure;

import io.github.gridadev.spring.vault.ssl.bundle.VaultSslBundleRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.core.VaultTemplate;

/**
 * Auto-configuration for Vault SSL Bundle support.
 * Enables SSL bundles to be loaded from HashiCorp Vault.
 */
@AutoConfiguration
public class VaultSslBundleAutoConfiguration {

    @Bean
    public VaultSslBundleRegistry vaultSslBundleRegistry(VaultTemplate vaultTemplate) {
        return new VaultSslBundleRegistry(vaultTemplate);
    }
}
