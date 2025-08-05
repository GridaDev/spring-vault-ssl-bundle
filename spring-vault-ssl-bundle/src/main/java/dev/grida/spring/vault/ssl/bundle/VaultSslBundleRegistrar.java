package dev.grida.spring.vault.ssl.bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Registers the VaultSslBundleRegistry with Spring's SSL bundle system.
 * This allows vault: prefixed bundle names to be resolved at runtime.
 */
@Component
public class VaultSslBundleRegistrar implements BeanFactoryPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(VaultSslBundleRegistrar.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        logger.debug("Registering Vault SSL Bundle support");

        // Register a custom SSL bundle registry that can handle vault: protocols
        beanFactory.registerSingleton("vaultSslBundleRegistryWrapper", new VaultSslBundleRegistryWrapper(beanFactory));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    /**
     * Wrapper that delegates to either the default registry or vault registry
     * based on the bundle name prefix.
     */
    private static class VaultSslBundleRegistryWrapper implements SslBundleRegistry {

        private final ConfigurableListableBeanFactory beanFactory;
        private SslBundleRegistry defaultRegistry;
        private VaultSslBundleRegistry vaultRegistry;

        public VaultSslBundleRegistryWrapper(ConfigurableListableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        public void registerBundle(String name, SslBundle bundle) {

            if (name.startsWith("vault:")) {
                logger.debug("Delegating to vault bundles for bundle: {}", name);
                getVaultRegistry().registerBundle(name, bundle);
            } else {
                logger.debug("Delegating to default bundles for bundle: {}", name);
                getDefaultRegistry().registerBundle(name, bundle);
            }
        }

        @Override
        public void updateBundle(String name, SslBundle updatedBundle) throws NoSuchSslBundleException {}

        private SslBundleRegistry getDefaultRegistry() {
            if (defaultRegistry == null) {
                try {
                    defaultRegistry = beanFactory.getBean(DefaultSslBundleRegistry.class);
                } catch (Exception e) {
                    logger.warn("Could not find DefaultSslBundleRegistry, creating new instance");
                }
            }
            return defaultRegistry;
        }

        private VaultSslBundleRegistry getVaultRegistry() {
            if (vaultRegistry == null) {
                try {
                    vaultRegistry = beanFactory.getBean(VaultSslBundleRegistry.class);

                } catch (Exception e) {
                    logger.warn("Could not find VaultSslBundleRegistry, creating new instance");
                }
            }
            return vaultRegistry;
        }
    }
}
