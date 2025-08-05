#!/bin/bash

# Vault initialization script
# This script is executed by the vault-init container to setup Vault with certificates

set -e

# Set Vault address and token
export VAULT_ADDR="http://127.0.0.1:8200"
export VAULT_TOKEN="demo-root-token"

echo "Environment variables set:"
echo "  VAULT_ADDR=$VAULT_ADDR"
echo "  VAULT_TOKEN=$VAULT_TOKEN"

echo "Waiting for Vault to be ready..."
until vault status > /dev/null 2>&1; do
    echo "Vault not ready, waiting..."
    sleep 2
done

echo "Vault is ready. Authenticating..."
vault login -method=token token=${VAULT_TOKEN}

echo "Enabling KV secrets engine..."
vault secrets enable -path=secret kv-v2 || echo "KV engine already enabled"

echo "Storing Server A certificates..."
vault kv put secret/ssl-certs/server-a \
    certificate="$(cat ./certificates/server-a-cert.pem)" \
    private-key="$(cat ./certificates/server-a-key.pem)" \
    ca-certificate="$(cat ./certificates/ca-cert.pem)"

echo "Storing Server B certificates..."
vault kv put secret/ssl-certs/server-b \
    certificate="$(cat ./certificates/server-b-cert.pem)" \
    private-key="$(cat ./certificates/server-b-key.pem)" \
    ca-certificate="$(cat ./certificates/ca-cert.pem)"

echo "Verifying stored certificates..."
vault kv get secret/ssl-certs/server-a
vault kv get secret/ssl-certs/server-b

echo "Vault initialization completed successfully!"