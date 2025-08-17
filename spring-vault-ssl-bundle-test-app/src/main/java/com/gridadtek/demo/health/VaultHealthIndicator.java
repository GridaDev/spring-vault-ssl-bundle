package com.gridadtek.demo.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;

/**
 * Health indicator for Vault connectivity and SSL bundle availability.
 */
@Component
public class VaultHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(VaultHealthIndicator.class);

    private final VaultTemplate vaultTemplate;

    public VaultHealthIndicator(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    @Override
    public Health health() {
        try {
            // Test basic Vault connectivity
            var sysHealth = vaultTemplate.opsForSys().health();

            // Test SSL certificate path accessibility
            var sslData = vaultTemplate.read("secret/data/ssl-certs/server-a");

            return Health.up()
                    .withDetail("vault.status", "UP")
                    .withDetail("vault.initialized", sysHealth.isInitialized())
                    .withDetail("vault.sealed", sysHealth.isSealed())
                    .withDetail("ssl.bundle.available", sslData != null)
                    .withDetail("ssl.bundle.path", "secret/data/ssl-certs/server-a")
                    .build();

        } catch (Exception e) {
            logger.error("Vault health check failed", e);
            return Health.down()
                    .withDetail("vault.status", "DOWN")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
