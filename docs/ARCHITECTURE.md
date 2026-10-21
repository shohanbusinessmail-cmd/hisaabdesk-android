# HisaabDesk — Architecture

## Overview
Single-module native Android app (`:app`), Kotlin + Jetpack Compose (Material 3), offline-first. No backend, no paid APIs. All financial data lives in a Room (SQLite) database on-device.

## Layers
- `ui/` — Compose screens + Navigation Compose. Screens observe `StateFlow` UI state; no business logic in composables beyond validation display.
- `feature/viewmodel` — `MainViewModel` exposes flows from repositories and one-shot suspend actions. (Single shared VM keeps 1.0.0 scope reviewable; feature VMs can be split without data-layer change.)
- `domain/calculator` — pure-Kotlin, unit-tested financial math (loan EMI/flat interest, budget, net worth). No Android imports.
- `data/repository` — `FinanceRepository`: the only writer of financial records. Multi-record operations run inside `RoomDatabase.withTransaction`.
- `data/local` — Room entities, DAOs, `HisaabDatabase` (versioned, explicit migrations, never destructive).
- `core/` — money/date formatting, PIN security (PBKDF2), backup/restore (JSON + AES-GCM), notifications (WorkManager).

## Navigation
Bottom bar: ড্যাশবোর্ড (dashboard) · লেনদেন (transactions) · অ্যাকাউন্ট (accounts) · আরও (more hub → shops, debts, NGO loans, bank loans, savings, budgets, categories, reports, settings, about). Onboarding and PIN-lock are top-level gates before the main graph.

## Database & relationships
22 tables (see `docs/FINANCIAL_RULES.md` §Data): accounts, ledger_entries, categories, subcategories, shops, shop_purchases(+items), shop_payments, persons, personal_debts(+payments), ngo_loans(+installments, payments), bank_loans(+schedule, payments), savings_accounts(+movements), budgets, recurring_transactions, scheduled_reminders. Foreign keys use RESTRICT/SET NULL deliberately so history is never cascade-deleted; user-facing "delete" of referenced entities is archival.

## Ledger model
Every cash movement writes `ledger_entries` rows (signed poisha, typed). Transfers write two rows in one transaction sharing a `groupId`; their sum is zero. Balances = opening balance + Σ ledger rows. Reports classify by entry type — transfers/savings moves/loan principal never count as income/expense.

## Migrations
Schema v1 is the baseline. Any future change: bump version, add explicit `Migration`, add a migration test. `fallbackToDestructiveMigration` is never used.

## Backup format
JSON snapshot {backupVersion, schemaVersion, exportedAt, sha256, payload:{...tables}}; optional password-encrypted form: PBKDF2-HMAC-SHA256 → AES-256-GCM. Restore validates checksum + versions before touching the DB, then replaces data in one transaction. See `docs/SECURITY.md`.

## Tests
Pure JVM unit tests for money/loans/budgets/dates (`app/src/test`). Room instrumentation tests are declared in TEST_PLAN (require emulator; not run in this sandbox/CI free tier beyond compile).

## CI/CD
`.github/workflows/android-ci.yml`: JDK 17, unit tests, lint, debug APK artifact. `release.yml`: tag `v*` → tests, release APK (unsigned unless signing secrets configured — stated honestly in release notes), SHA-256, GitHub Release. See `docs/RELEASE_PROCESS.md`.
