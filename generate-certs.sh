#!/bin/bash

# Certificate Generation Script for Vault SSL Demo
# This script creates a CA and server certificates for the demo

CERT_DIR="certificates"
mkdir -p $CERT_DIR

echo "Generating certificates for Vault SSL Demo..."

# Generate CA private key
openssl genrsa -out $CERT_DIR/ca-key.pem 4096

# Generate CA certificate
openssl req -new -x509 -days 365 -key $CERT_DIR/ca-key.pem -out $CERT_DIR/ca-cert.pem \
    -subj "/C=US/ST=Demo/L=Demo/O=Demo/OU=Demo/CN=Demo-CA"

# Generate Server A private key
openssl genrsa -out $CERT_DIR/server-a-key.pem 2048

# Generate Server A certificate signing request
openssl req -new -key $CERT_DIR/server-a-key.pem -out $CERT_DIR/server-a.csr \
    -subj "/C=US/ST=Demo/L=Demo/O=Demo/OU=Demo/CN=server-a"

# Create extensions file for Server A
cat > $CERT_DIR/server-a.ext << EOF
[v3_req]
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = server-a
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF

# Generate Server A certificate
openssl x509 -req -in $CERT_DIR/server-a.csr -CA $CERT_DIR/ca-cert.pem -CAkey $CERT_DIR/ca-key.pem \
    -CAcreateserial -out $CERT_DIR/server-a-cert.pem -days 365 -extensions v3_req \
    -extfile $CERT_DIR/server-a.ext

# Generate Server B private key
openssl genrsa -out $CERT_DIR/server-b-key.pem 2048

# Generate Server B certificate signing request
openssl req -new -key $CERT_DIR/server-b-key.pem -out $CERT_DIR/server-b.csr \
    -subj "/C=US/ST=Demo/L=Demo/O=Demo/OU=Demo/CN=server-b"

# Create extensions file for Server B
cat > $CERT_DIR/server-b.ext << EOF
[v3_req]
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = server-b
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF

# Generate Server B certificate
openssl x509 -req -in $CERT_DIR/server-b.csr -CA $CERT_DIR/ca-cert.pem -CAkey $CERT_DIR/ca-key.pem \
    -CAcreateserial -out $CERT_DIR/server-b-cert.pem -days 365 -extensions v3_req \
    -extfile $CERT_DIR/server-b.ext

# Clean up CSR and extension files
rm $CERT_DIR/*.csr
rm $CERT_DIR/*.ext
rm $CERT_DIR/*.srl

echo "Certificates generated successfully in $CERT_DIR directory:"
echo "- CA Certificate: ca-cert.pem"
echo "- Server A Certificate: server-a-cert.pem"
echo "- Server A Private Key: server-a-key.pem"
echo "- Server B Certificate: server-b-cert.pem"
echo "- Server B Private Key: server-b-key.pem"

# Verify certificates
echo -e "\nVerifying certificates..."
openssl verify -CAfile $CERT_DIR/ca-cert.pem $CERT_DIR/server-a-cert.pem
openssl verify -CAfile $CERT_DIR/ca-cert.pem $CERT_DIR/server-b-cert.pem

echo -e "\nCertificate generation complete!"
echo "You can now run 'docker-compose up' to start Vault and load these certificates."