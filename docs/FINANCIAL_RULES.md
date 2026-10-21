# Financial Rules (আর্থিক নিয়ম)

## Money representation
- All amounts are `Long` **poisha** (৳1 = 100 poisha). No `Float`/`Double` anywhere in monetary paths.
- Parsing accepts Bengali (০–৯) and Latin digits, optional `৳`, commas, and at most 2 decimal places; anything else is rejected with a Bengali validation message. Overflow is checked (`Math.multiplyExact/addExact`).
- Display: Bengali numerals, lakh-style grouping, `৳` prefix; poisha shown only when non-zero (lists) / always in detail.
- Rates are stored as scaled integers (basis points / per-myriad) or `BigDecimal` strings; loan math uses `BigDecimal` with `HALF_UP` rounding to poisha, documented per method.

## Event classification (ledger types)
INCOME, EXPENSE, TRANSFER_IN, TRANSFER_OUT, SAVINGS_DEPOSIT, SAVINGS_WITHDRAW, LOAN_RECEIVED, LOAN_GIVEN, DEBT_REPAYMENT_OUT, DEBT_REPAYMENT_IN, SHOP_CREDIT_PURCHASE (non-cash, no ledger cash row until paid), SHOP_PAYMENT, NGO_DISBURSEMENT, NGO_INSTALLMENT, BANK_DISBURSEMENT, BANK_INSTALLMENT, FEE_INTEREST, OPENING_BALANCE (account opening field, not income), REVERSAL.

## Non-double-counting rules
- Transfer: −source/+destination, same groupId, Σ=0; excluded from income & expense totals.
- Savings deposit/withdrawal between own accounts: transfer-class; excluded from income/expense and net-worth-neutral.
- Loan disbursement: +cash, +liability; NOT income. Principal repayment: −cash, −liability; NOT expense. Interest/fees paid: EXPENSE-class (`FEE_INTEREST`) only when actually paid, once.
- Money lent: −cash, +receivable; repayment received: +cash, −receivable; principal is never income.
- Shop purchase on credit: expense recognised on purchase date (category-linked ledger-independent purchase record + liability). The later shop payment only reduces liability (SHOP_PAYMENT) — never a second expense. Split cash+credit purchase: cash part is an expense row, credit part creates liability, single purchase record.
- Budget actuals count EXPENSE (purchase-date for credit purchases) only; transfers, principal repayments and savings moves are excluded.

## Loans
- NGO: never invent charges. Total payable = principal + user-entered service charge + user-entered other charges; if not supplied, UI shows "জানা নেই". Remaining obligation and remaining principal are shown separately.
- Bank: three explicit modes — (a) REDUCING_BALANCE: EMI = P·r/(1−(1+r)^−n), r = annualRate/12 (documented conversion), BigDecimal HALF_UP; (b) FLAT: interest = P × rate × years, evenly spread; (c) MANUAL: user-entered schedule preserved verbatim, no inferred split. Missing rate/tenure ⇒ manual mode required; the app never guesses.
- Overpayment of any debt/installment is rejected unless the user confirms an explicit excess handling (recorded as note + capped at outstanding by default).

## Net worth
Assets = liquid account balances + savings balances not already inside those accounts + outstanding receivables (personal lending + shop receivables n/a). Liabilities = shop outstanding + personal borrowed outstanding + NGO remaining obligation + bank remaining obligation (principal where separable, else contractual remaining — labelled). Net worth = assets − liabilities. A savings account that is also a financial account is counted once (savings balances live in their own table and are excluded from account-balance asset sum when linked — in 1.0.0 savings are separate records, never linked as accounts, so no double count).

## Dates
Epoch millis stored; display in Asia/Dhaka; user picks explicit local date. Day boundaries computed in Asia/Dhaka.
