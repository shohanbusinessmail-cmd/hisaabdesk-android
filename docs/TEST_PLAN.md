# Test Plan

## Unit tests (JVM, run in CI: `./gradlew testDebugUnitTest`)
- `MoneyTest`: Bengali/Latin parsing, poisha conversion, rejection of 3-decimals/letters/overflow, formatting round-trip, lakh grouping.
- `LoanCalculatorTest`: reducing-balance EMI known value, flat interest, manual mode yields no inferred split, zero-rate edge, rounding HALF_UP.
- `FinanceCalculatorTest`: fixture from spec — opening ৳1,000 + income ৳500 − expense ৳200 − transfer-out ৳100 ⇒ cash ৳1,200, bank +৳100, income ৳500, expense ৳200, transfer in income/expense ৳0.
- Shop balance & partial payment, personal debt partial repayment & overpayment guard, NGO remaining obligation vs principal, budget utilisation & overspend, net worth, date-range boundaries (Asia/Dhaka), recurring occurrence id stability/dedup.

## Database tests (instrumented, `androidTest`, need emulator — NOT run in sandbox)
Insert/read/update/archive, FK restrict behaviour, transaction rollback on failed transfer, reopen persistence, backup→restore round-trip, restore checksum failure leaves DB untouched, duplicate occurrence prevention.

## UI tests (instrumented, need emulator — NOT run in sandbox)
Onboarding PIN setup/confirm, wrong PIN, unlock, empty dashboard, create account, add income/expense, shop + partial payment, personal debt + repayment, NGO/bank loan creation, savings movement, report rendering, backup export/restore, About screen.

## Manual smoke (release gate)
Fresh install → onboarding → create cash account (opening ৳0) → dashboard shows ৳০ everywhere, no sample data → add income/expense → restart app → data persists → PIN lock on relaunch.

## Honesty rule
A test is reported PASS only with executed output. Instrumented tests are marked NOT RUN where no emulator exists.
