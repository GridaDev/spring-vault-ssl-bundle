# 🔐 Spring Vault SSL Bundle

> Seamlessly integrate HashiCorp Vault with Spring Boot SSL configuration using native `ssl.bundle` support.

## ❌ The Problem

Spring Boot's built-in SSL bundle support is powerful but has limitations when working with HashiCorp Vault:

| Issue | Impact |
|-------|--------|
| **No Vault Integration** | Must manually fetch certificates from Vault using scripts or CLI |
| **File System Dependency** | Requires storing PEM files locally in resources or filesystem |
| **Manual Certificate Rotation** | No automation for certificate lifecycle management |
| **DevOps Overhead** | Additional tooling and processes required |

### Traditional Approach
```yaml
# ❌ Requires manual certificate management
spring:
  ssl:
    bundle:
      pem:
        mybundle:
          keystore:
            certificate: classpath:server.crt  # Must be manually placed
            private-key: classpath:server.key  # Must be manually rotated
```

## ✅ The Solution

**spring-vault-ssl-bundle** bridges this gap by extending Spring Boot's SSL bundle system with native Vault support.

### Key Benefits

🚀 **Zero File Management** - Certificates are fetched directly from Vault  
🔄 **Automatic Rotation Ready** - Built for dynamic certificate workflows  
🎯 **Native Integration** - Works seamlessly with existing `ssl.bundle` configuration  
⚡ **Zero Code Changes** - Just update your YAML configuration  
🔒 **Secure by Design** - Leverages Vault's security model

### Modern Approach
```yaml
# ✅ Direct Vault integration
server:
  ssl:
    bundle: "vault:secret/data/ssl-certs/my-service"
```

## 🚀 Quick Start

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>io.github.gridadev</groupId>
    <artifactId>spring-vault-ssl-bundle</artifactId>
    <version>0.0.1</version>
</dependency>
```

**Gradle:**
```kotlin
implementation("io.github.gridadev:spring-vault-ssl-bundle:0.0.1")
```

### 2. Store Certificates in Vault

```bash
# Store your SSL certificates in Vault
vault kv put secret/ssl-certs/my-service \
  certificate=@server.crt \
  private_key=@server.key \
  ca_certificate=@ca.crt
```

### 3. Configure Application

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    bundle: "vault:secret/data/ssl-certs/my-service"

spring:
  application:
    name: my-service
  cloud:
    vault:
      host: localhost
      port: 8200
      scheme: http
      authentication: TOKEN
      token: ${VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
```

That's it! Your application now loads SSL certificates directly from Vault.

## 📋 Certificate Format

Store certificates in Vault using this JSON structure:

```json
{
  "certificate": "-----BEGIN CERTIFICATE-----\nMIIC...\n-----END CERTIFICATE-----",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----",
  "ca_certificate": "-----BEGIN CERTIFICATE-----\nMIIC...\n-----END CERTIFICATE-----"
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `certificate` | ✅ | Server certificate in PEM format |
| `private_key` | ✅ | Private key in PEM format |
| `ca_certificate` | ❌ | CA certificate chain (optional) |

## ⚙️ Configuration Options

### Bundle URI Format
```
vault:<vault-path>
```

### Examples
```yaml
# Different services, different certificates
server:
  ssl:
    bundle: "vault:secret/data/ssl-certs/api-gateway"

# Or for different environments
server:
  ssl:
    bundle: "vault:secret/data/ssl-certs/${spring.profiles.active}/web-server"
```

## 🆚 Comparison

| Feature | Native Spring Boot | spring-vault-ssl-bundle |
|---------|-------------------|-------------------------|
| **Vault Integration** | ❌ Manual scripts | ✅ Native support |
| **File Management** | ❌ Required | ✅ Eliminated |
| **Certificate Rotation** | ❌ Manual process | ✅ Automation-ready |
| **Configuration** | ⚠️ Complex | ✅ Simple |
| **Security** | ⚠️ Files on disk | ✅ Vault-secured |
| **DevOps Friendly** | ❌ Extra tooling | ✅ Built-in |

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

