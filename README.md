# Spring  Vault SSL Bundle

This project enables loading SSL certificates from HashiCorp Vault using the `vault:` prefix in your SSL bundle configuration.

## Features

- Seamless integration with Spring Boot's SSL bundle system
- Automatic loading of certificates from Vault at startup
- Support for certificate, private key, and CA certificate
- Caching of loaded certificates for performance
- Compatible with Spring Cloud Vault configuration

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.gridadev</groupId>
    <artifactId>spring-vault-ssl-bundle</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Vault Certificate Format

Store your SSL certificates in Vault with the following structure:

```json
{
  "certificate": "-----BEGIN CERTIFICATE-----\nMIIE...\n-----END CERTIFICATE-----",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----",
  "ca_certificate": "-----BEGIN CERTIFICATE-----\nMIIE...\n-----END CERTIFICATE-----"
}
```

### Required Fields:
- `certificate`: The server certificate in PEM format
- `private_key`: The private key in PEM format (PKCS#8 or RSA format)

### Optional Fields:
- `ca_certificate`: The CA certificate in PEM format

## Configuration

### Application Properties

Your `application.yml` can reference Vault-stored certificates using the `vault:` prefix:

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    bundle: "vault:secret/data/ssl-certs/server-a"

spring:
  application:
    name: server-a
  cloud:
    vault:
      host: localhost
      port: 8200
      scheme: http
      authentication: TOKEN
      token: demo-root-token
      kv:
        enabled: true
        backend: secret
        default-context: ssl-certs
```

### Bundle Name Format

The bundle name must follow this pattern:
```
vault:<vault-path>
```

Examples:
- `vault:secret/data/ssl-certs/server-a`


### Export GPG private key and public key 

```
gpg --armor --export YOUR_KEY_ID > public.key
gpg --armor --export-secret-key YOUR_KEY_ID  > secret.key
```

### License
MIT License.



