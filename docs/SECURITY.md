# Security

## App PIN
- 4–6 digit PIN chosen at onboarding, confirmed twice. Stored **only** as PBKDF2-HMAC-SHA256 (120,000 iterations, 16-byte `SecureRandom` salt, 256-bit output), salt+hash in DataStore Preferences. Plain PIN never persisted or logged.
- Verification recomputes and compares with `MessageDigest.isEqual` (constant-time). Failed attempts: exponential back-off delay in UI (1s doubling, cap 30s); data is never wiped on wrong PIN.
- Change PIN requires current PIN. Biometric unlock (AndroidX Biometric) is opt-in and always falls back to PIN.
- **Recovery limitation (honest):** there is no server and no identity provider. A forgotten PIN cannot be verified or reset remotely. Recovery = restore an exported backup into a fresh install (new PIN). This is stated in-app and in README.
- Lock: app re-locks when sent to background (configurable immediate/1-min in Settings) — implemented via lifecycle observer clearing the unlocked flag.

## Database at rest
Room/SQLite is **not** encrypted by default and this build does not claim otherwise. SQLCipher was evaluated and excluded from 1.0.0 to avoid an unmaintained native dependency risk; device-level encryption (Android FBE) + app PIN gate the data. This limitation is disclosed in-app (Settings → নিরাপত্তা) and README. Re-evaluate SQLCipher/community fork before 1.1.

## Backup encryption
Optional password encryption: PBKDF2-HMAC-SHA256 (150k iters, random 16-byte salt) → AES-256-GCM (random 12-byte IV), format `HDENC1:salt:iv:ciphertext` (Base64). Plain JSON/CSV exports trigger an explicit Bengali warning dialog. PIN/hash are never exported.

## Other
- No secrets in source; release signing only via GitHub Secrets. `android:allowBackup=false` with explicit backup rules excluding the DB from cloud auto-backup. No financial data in logs. Notifications show generic text (no amounts) by default.
