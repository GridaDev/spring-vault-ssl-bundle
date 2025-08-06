package io.github.gridadev.spring.vault.ssl.bundle.autoconfigure;

import io.github.gridadev.spring.vault.ssl.bundle.VaultSslBundleRegistrar;
import io.github.gridadev.spring.vault.ssl.bundle.VaultSslBundleRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.vault.core.VaultTemplate;

/**
 * Auto-configuration for Vault SSL Bundle support.
 * Enables SSL bundles to be loaded from HashiCorp Vault.
 */
@AutoConfiguration
@ConditionalOnClass({VaultTemplate.class, SslBundles.class})
@ConditionalOnProperty(prefix = "spring.cloud.vault", name = "enabled", matchIfMissing = true)
@Import(VaultSslBundleRegistrar.class)
public class VaultSslBundleAutoConfiguration {

    @Bean
    public VaultSslBundleRegistry vaultSslBundleRegistry(VaultTemplate vaultTemplate) {
        return new VaultSslBundleRegistry(vaultTemplate);
    }
}
