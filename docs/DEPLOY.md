# Deploying KisanSetu (single server)

This is a plain "one VM + Docker Compose" setup. It has not been run on a cloud account as part of this project;
treat it as a checklist and adjust. A managed MySQL (for example Amazon RDS) is a good upgrade: remove the `db`
service, point `DB_HOST` at it, and enable its automatic backups.

## 1. Server
- Ubuntu LTS VM (for example EC2 `t3.small`), Docker and the Compose plugin installed.
- Firewall / security group: allow 80 and 443 from anywhere, 22 only from your own IP. Do not open 8080 or 3306.
- A domain name whose DNS A record points at the server (Caddy needs this to get the HTTPS certificate).

## 2. Configure
    git clone <your repo> && cd kisansetu
    cp .env.example .env
    ./scripts/generate-secrets.sh        # paste the generated values into .env
    # also set in .env: DOMAIN, ADMIN_USERNAME, and (for the tracker) TWILIO_AUTH_TOKEN and
    # TWILIO_WEBHOOK_URL=https://<DOMAIN>/api/webhooks/twilio

Back up `APP_ENCRYPTION_KEY` and `APP_HMAC_KEY` somewhere safe and separate from the database backups.
Without the encryption key the stored Aadhaar and bank account numbers cannot be read; changing the HMAC key
breaks duplicate detection for already-saved farmers.

## 3. Start
    docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
    curl https://<DOMAIN>/actuator/health        # {"status":"UP"}

Log in at https://<DOMAIN>/ with ADMIN_USERNAME / ADMIN_PASSWORD, then create operator accounts (POST /api/users).
After the first start you can remove ADMIN_PASSWORD from `.env`; it is only used while no user exists.

## 4. Backups
    docker compose exec -T db sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction kisansetu' | gzip > kisansetu-$(date +%F).sql.gz
Run this from cron and copy the file off the server. Test a restore at least once.

## 5. Updating
    git pull && docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
Flyway applies new migrations on start-up. Never edit a migration that has already been applied; add a new one.

## Limits to know about
- Login and WhatsApp rate limits are kept in memory, so they assume a single app instance.
- JWTs cannot be revoked before they expire (60 minutes); deleting a user does not end a live session.
