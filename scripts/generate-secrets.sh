#!/usr/bin/env bash
# Prints random values for the secrets in .env. Copy the lines you need into .env.
set -euo pipefail

cat <<VALUES
DB_PASSWORD=$(openssl rand -hex 24)
DB_ROOT_PASSWORD=$(openssl rand -hex 24)
APP_ENCRYPTION_KEY=$(openssl rand -base64 32)
APP_HMAC_KEY=$(openssl rand -base64 32)
APP_JWT_SECRET=$(openssl rand -base64 32)
ADMIN_PASSWORD=$(openssl rand -base64 18)
VALUES
