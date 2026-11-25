# KisanSetu

A Data Validation and Status-Tracking Platform for Interest Subvention & Prompt Repayment
Incentive Claims under the Kisan Credit Card Scheme.

Bank and CSC staff upload a claims sheet; KisanSetu auto-formats it, checks Aadhaar / KYC / land-record data,
returns a clear error report, and saves the rows that pass. Farmers can ask for their claim status by SMS or
WhatsApp, with the reason if it was rejected.

## Status
- Phase 0-1: skeleton, Docker Compose, Flyway, data model - done
- Phase 2: validation engine (auto-format, 20+ rules, duplicate and cross-record checks) - done
- Phase 3: .xlsx/.csv upload, stored error list, downloadable error report - done
- Phase 4: JWT login + roles, Aadhaar and bank account encryption, saving valid rows, audit log - done
- Phase 5: claim status updates, claims list, SMS/WhatsApp status tracker (Twilio) - done
- Phase 6: CI security pipeline, hardened containers, HTTPS overlay, deploy guide - done
- Phase 7: three-page web UI (login, upload + errors, claims) - done

## Quick start
    cp .env.example .env
    ./scripts/generate-secrets.sh     # paste the output into .env, set ADMIN_USERNAME
    docker compose up --build
    open http://localhost:8080/       # log in with ADMIN_USERNAME / ADMIN_PASSWORD

The app starts only if APP_ENCRYPTION_KEY, APP_HMAC_KEY and APP_JWT_SECRET are set (base64, 32+ bytes).
It listens on 127.0.0.1 only; use `docker-compose.prod.yml` (see docs/DEPLOY.md) for HTTPS on a server.

Try it with the sample file: log in, upload `samples/sample_claims.xlsx` (tick "Check only" first to see the
result without saving). Expected: 100 rows, 85 valid, 15 with errors, exactly the rows in `samples/EXPECTED_ERRORS.md`.
Ten more rows in the sample are messy but valid (spaces in Aadhaar, +91 in mobile, lowercase IFSC, dd-mm-yyyy
dates, "Rs. 1,50,000", "2025-26") and are fixed silently. Upload the same file again and every saved row is
reported as an already-saved claim.

## Roles
| Role | Can do |
|---|---|
| BANK_ADMIN | everything: upload, view, change claim status, create users |
| CSC_OPERATOR | upload files, view batches and claims |

## API
All `/api/**` calls need `Authorization: Bearer <token>` except login and the Twilio webhook.

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | /api/auth/login | anyone | `{"username","password"}` -> token (60 min). 5 failures per username+address in 15 min -> 429 |
| POST | /api/users | admin | create a user `{"username","password"(12+ chars),"role"}` |
| POST | /api/batches?dryRun=false | staff | multipart `file` (.xlsx first sheet / .csv, 5 MB, 5000 rows). Valid rows are saved; `dryRun=true` only checks |
| GET | /api/batches/{id} | staff | counts and status (VALIDATED = checked only, COMMITTED = saved) |
| GET | /api/batches/{id}/errors | staff | JSON list: row, field, code, message, suggestion |
| GET | /api/batches/{id}/error-report | staff | the same list as an .xlsx download |
| GET | /api/claims?status=&page=&size= | staff | claims, newest first (no Aadhaar, mobile or bank data) |
| PATCH | /api/claims/{id}/status | admin | `{"status":"APPROVED\|PENDING\|REJECTED","rejectionReason"}`; a reason is required for REJECTED |
| POST | /api/webhooks/twilio | Twilio | incoming SMS/WhatsApp message; must carry a valid X-Twilio-Signature |

Errors: 400 bad input, 401 not logged in, 403 wrong role, 404 not found, 409 conflict, 413 file too large, 429 too many attempts.

## SMS / WhatsApp tracker
A farmer sends `STATUS MP2025000101` (or just the number) to the Twilio number. KisanSetu replies with each claim's
status and the rejection reason. The reply is given only if the sender's number matches the mobile number saved for
that KCC account; a wrong number and an unknown account get the identical reply, so accounts cannot be probed.
Each phone number is limited to 10 requests an hour.

Setup (WhatsApp sandbox is the easiest for a demo): in the Twilio console set the incoming-message webhook to
`https://<your domain>/api/webhooks/twilio` (HTTP POST), put the console's Auth Token in `TWILIO_AUTH_TOKEN`, and set
`TWILIO_WEBHOOK_URL` to exactly that URL (Twilio signs it). Locally, use a tunnel (ngrok, Cloudflare Tunnel) and the
tunnel URL. Real SMS to Indian numbers needs DLT registration, which is outside this project; check Twilio's current rules.

Try the webhook by hand (parameters sorted by name, each name directly followed by its value, no '=' or '&'):

    URL=https://<your domain>/api/webhooks/twilio
    DATA="${URL}BodySTATUS MP2025000101Fromwhatsapp:+919876543210"
    SIG=$(printf '%s' "$DATA" | openssl dgst -sha1 -hmac "$TWILIO_AUTH_TOKEN" -binary | base64)
    curl -X POST "$URL" -H "X-Twilio-Signature: $SIG" \
         --data-urlencode "From=whatsapp:+919876543210" --data-urlencode "Body=STATUS MP2025000101"

## Security design
- **Encrypted at rest:** Aadhaar number and bank account number, AES-256-GCM per value with a random IV; each
  ciphertext is bound to its column, so it cannot be copied into another one. Format `v1:` + base64, so keys can be rotated later.
- **Duplicate lookup without decrypting:** a keyed hash (HMAC-SHA256) of the Aadhaar, not a plain hash.
- **Not encrypted:** names, mobile numbers, land record IDs, IFSC, amounts. They are needed for lookups and matching.
  Use disk/volume encryption on the server for the rest. The Aadhaar is never shown in full in messages, reports or API output.
- **In transit:** HTTPS through Caddy (`docker-compose.prod.yml`); the app itself speaks HTTP behind it.
- **Auth:** BCrypt passwords (12+ characters), stateless JWT (HS256, 60 min), role checks in one place (`SecurityConfig`),
  login lock-out, constant-time failure path for unknown usernames, audit log of logins, uploads, status changes and user creation.
- **Secrets:** environment only; the app refuses to start without them. Webhook requests are rejected without a valid Twilio signature.
- **Containers:** non-root user, read-only root filesystem, all capabilities dropped, ports bound to localhost.
- **Pipeline:** tests, gitleaks, Semgrep, Trivy (dependencies, config, image), Dependabot. The image is pushed only after all pass.
- **Web UI:** server text is inserted as text, never HTML; a Content-Security-Policy blocks inline scripts.

## Upload file format
Header row required; names match ignoring case, spaces and hyphens. One row = one claim.
Required: kcc_account_no, full_name, date_of_birth, mobile, aadhaar, kyc_name, bank_account_no, ifsc, land_record_id,
land_owner_name, area_hectares, claim_type (IS/PRI), financial_year, loan_amount, claim_amount.
Optional: father_name, kyc_dob, village, district, state.
In Excel, format Aadhaar / mobile / account-number columns as Text, otherwise Excel may drop leading zeros.

## Validation rules (`src/main/java/com/kisansetu/validation`)
Aadhaar 12 digits + Verhoeff checksum; mobile; IFSC; bank account; KCC account; land record ID; date of birth (valid, age 18-100);
claim type; financial year; area and amounts (claim <= loan); KYC name vs claim name (fuzzy); land owner vs farmer or
father (fuzzy); KYC date of birth vs date of birth; duplicate claim, Aadhaar under two KCC accounts, KCC account under
two Aadhaar numbers - inside the file and against already-saved data.

## Tests
    mvn test        # needs Docker running (Testcontainers MySQL)
- Plain unit tests: Verhoeff, name matching, normalizer, every rule, conflict checks, encryption, HMAC, rate limiter,
  Twilio signature, status messages, header mapping.
- Integration tests (MySQL in Docker): schema migration, upload and error report, login and roles, encrypted storage,
  duplicate handling, status updates, the WhatsApp tracker (signature, number matching, rate limit).

## Assumptions and limits
- One bank / organization per deployment: every staff user sees every batch and claim. There is no per-branch separation.
- All data in this repository is synthetic. KisanSetu does not talk to the real Kisan Rin Portal, UIDAI or state land
  record systems; handling real Aadhaar data brings legal obligations that this project does not address.
- Claim status is entered by a BANK_ADMIN; nothing reads it from the portal.
- Re-uploading an existing farmer reuses the saved record and does not overwrite it. There is no farmer-edit endpoint, so a
  changed mobile number cannot be corrected through the API yet (the tracker matches on the saved number).
- KCC account and land record ID formats are generic (they differ by bank and state); name-match thresholds are 0.88 (KYC)
  and 0.85 (land owner); scheme-specific loan caps are not hard-coded. Change them in the `validation` package.
- Rate limits are in memory (single instance). JWTs cannot be revoked before they expire.
