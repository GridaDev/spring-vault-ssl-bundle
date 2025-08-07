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
⚡ **Smart Caching** - Eliminates duplicate Vault calls within the same bundle  
🎯 **Flexible Configuration** - Simple for common cases, advanced when needed

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
  private-key=@server.key \
  ca-certificate=@ca.crt
```

### 3. Configure Application

**Simple Configuration (Recommended):**
```yaml
spring:
  ssl:
    bundle:
      pem:
        my-service:
          keystore:
            certificate: "vault:secret/data/ssl-certs/my-service"
          truststore:
            certificate: "vault:secret/data/ssl-certs/my-service"

server:
  port: 8443
  ssl:
    enabled: true
    bundle: my-service

spring:
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
  "private-key": "-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----",
  "ca-certificate": "-----BEGIN CERTIFICATE-----\nMIIC...\n-----END CERTIFICATE-----"
}
```

**Field Requirements by Use Case:**
- **Keystore Configuration**: `certificate` is required, `private-key` and `ca-certificate` are filled automatically unless explicitly specified.
- **Truststore only Configuration**: Only `ca-certificate` is required (or certificate as fallback)

## ⚙️ Configuration Options

### 🎯 Simple Configuration (Recommended)

Specify the Vault path once - the system automatically maps standard field names:

```yaml
spring:
  ssl:
    bundle:
      pem:
        web-server:
          keystore:
            certificate: "vault:secret/data/ssl-certs/web-server"
          truststore:
            certificate: "vault:secret/data/ssl-certs/web-server"
```

**Automatic Field Mapping:**
- `certificate` → Server certificate
- `private-key` → Private key
- `ca-certificate` → CA certificate (with fallback to `certificate`)

### 🔧 Advanced Configuration

When you need custom field names or separate paths:

```yaml
spring:
  ssl:
    bundle:
      pem:
        api-gateway:
          keystore:
            certificate: "vault:secret/data/ssl-certs/api-gateway:server_cert"
            private-key: "vault:secret/data/private-keys/api-gateway:server_key"
          truststore:
            certificate: "vault:secret/data/ca-certs/root:ca_bundle"
```

### 🔒 Truststore-Only Configuration

For client applications that only need to validate server certificates:

```yaml
spring:
  ssl:
    bundle:
      pem:
        client-app:
          truststore:
            certificate: "vault:secret/data/ca-certs/trusted-cas"
```

### 🌐 Multiple Bundle Configuration

Different services can have different certificate configurations:

```yaml
spring:
  ssl:
    bundle:
      pem:
        # Internal service communication
        internal:
          keystore:
            certificate: "vault:secret/data/ssl-certs/internal"
          truststore:
            certificate: "vault:secret/data/ssl-certs/internal"
        
        # External client connections  
        external:
          keystore:
            certificate: "vault:secret/data/ssl-certs/external"
          truststore:
            certificate: "vault:secret/data/ca-certs/public-cas"
```

## 🏗️ Vault Path Format

### Basic Syntax
```
vault:<vault-path>
```

### Advanced Syntax
```  
vault:<vault-path>:<field-name>
```

### Examples
```yaml
# Use default field names
certificate: "vault:secret/data/ssl-certs/my-app"

# Specify custom field name
certificate: "vault:secret/data/ssl-certs/my-app:server_certificate" 

# Different paths for different components
certificate: "vault:pki/cert/my-app:certificate"
private-key: "vault:pki/private/my-app:private_key"
```

## 🔍 Vault Secret Engine Support

### KV Version 2 (Recommended)
```bash
# Enable KV v2 
vault secrets enable -path=secret kv-v2

# Store certificate
vault kv put secret/ssl-certs/my-service \
  certificate=@server.crt \
  private-key=@server.key
```

### KV Version 1 (Legacy)
```bash
# Enable KV v1
vault secrets enable -path=secret kv

# Store certificate  
vault kv put secret/ssl-certs/my-service \
  certificate=@server.crt \
  private-key=@server.key
```

The library automatically detects and handles both KV v1 and KV v2 formats.

## 🆚 Comparison

| Feature | Native Spring Boot | spring-vault-ssl-bundle |
|---------|-------------------|-------------------------|
| **Vault Integration** | ❌ Manual scripts | ✅ Native support |
| **File Management** | ❌ Required | ✅ Eliminated |
| **Certificate Rotation** | ❌ Manual process | ✅ Automation-ready |
| **Configuration** | ⚠️ Complex | ✅ Simple |
| **Security** | ⚠️ Files on disk | ✅ Vault-secured |
| **DevOps Friendly** | ❌ Extra tooling | ✅ Built-in |
| **Performance** | ✅ File system | ✅ Smart caching |
| **Flexibility** | ❌ Static files | ✅ Dynamic configuration |

## 🛠️ Requirements

- **Java**: 17 or higher
- **Spring Boot**: 3.0 or higher
- **Spring Vault**: 3.0 or higher
- **HashiCorp Vault**: Any supported version

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Made with ❤️ for the Spring Boot community**