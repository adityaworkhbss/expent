# Expent – Data Model (current vs target)

## 1. What Expent is

- Everything is **monthly**. No daily / weekly / yearly periods anywhere.
- Goal: know **incoming** (source + amount) and **outgoing** (source + amount) for
  **previous month, this month, next month**, and later **predict** future months.
- Offline-first: Room is the source of truth, WorkManager pushes changes. The sync columns
  (`syncStatus`, `isDeleted`) and the `pending_sync` table stay on every mutable entity.
- `User` is done and not covered here.

Backend note: the backend is `C:\Users\Aditya Sharma\GolandProjects\expent-backend` (Go + Prisma,
`prisma/schema.prisma`). The column lists below were first derived from the app's Retrofit DTOs
(`data/remote/dto`); the Prisma schema is the source of truth for what the server stores.

---

## 2. The 5 entities – target

Every row also carries the app-only sync columns: `syncStatus` (SYNCED / PENDING_CREATE /
PENDING_UPDATE / PENDING_DELETE) and `isDeleted`. The backend does not need those.

| Entity | Target columns | Meaning |
|---|---|---|
| **Account** | id, userId, type, name | What money moved through. `type` = `PAY_NOW` / `PAY_LATER` |
| **Category** | id, userId, name, type | What kind of money. `type` = `INCOME` / `EXPENSE` |
| **Transaction** | id, userId, accountId, categoryId, amount, date | One actual income or expense (salary, bonus, EMI paid, grocery…) |
| **Budget** | id, userId, date, categoryId, amount | A recurring **monthly income** (salary, freelance…). Not a spending limit |
| **Expense** | id, userId, date, amount, categoryId, tenure, paid, name | A recurring **monthly outgoing**: EMI / loan / saving (tenure) or subscription / rent (no end) |

### Expense rules
- `tenure = null` → every month, no end (subscription, rent, SIP). **Must be null, never 0.**
- `tenure = N` → EMI for N months.
- `paid` = months already paid. Can go above `tenure` (it keeps counting); the EMI is finished once
  `paid >= tenure`.

---

## 3. Current state vs target

### Account – ✅ matches
| | Columns |
|---|---|
| Room `accounts` | id, name, type, userId, syncStatus, isDeleted |
| Backend (DTO) | id, name, type, userId |

### Category – ✅ matches
| | Columns |
|---|---|
| Room `categories` | id, name, type, userId, syncStatus, isDeleted |
| Backend (DTO) | id, name, type, userId, color, icon (unused by app) |

### Transaction – ⚠️ extra columns
| | Columns |
|---|---|
| Room `transactions` | id, userId, accountId, categoryId, **transferToAccountId, type**, amount, **transactionDate**, **note, merchant, paymentMethod, tags, status, isSalary**, isDeleted (nullable), **createdAt, updatedAt**, categoryName, accountName, transferToAccountName, syncStatus |
| Backend (DTO) | same plus `timestamp`/`date`/`transactionDate` (three date fields!) and `description`/`notes`/`note` |
| Target | id, userId, accountId, categoryId, amount, date |

Gaps:
- `type` duplicates `category.type` → can be derived (see open question 3).
- `transferToAccountId`, `tags`, `status`, `isSalary`, `merchant`, `paymentMethod` not in target.
- Backend returns three date fields and three note fields; pick one each.
- `isDeleted` is nullable here but non-null on every other entity.
- `categoryName` / `accountName` are cached for display; fine to keep in Room only (a JOIN would also work).

### Budget – ❌ shape differs
| | Columns |
|---|---|
| Room `budgets` | id, userId, categoryId, **periodType, limitAmount, startDate, endDate**, createdAt, updatedAt, categoryName, syncStatus, isDeleted |
| Backend (DTO) | id, userId, categoryId, periodType, limitAmount, startDate, endDate, createdAt, updatedAt, category{} |
| Target | id, userId, date, categoryId, amount |

Gaps:
- Remove `periodType` (always monthly). The onboarding UI still offers WEEKLY / MONTHLY.
- `limitAmount` → `amount` (it is income, not a limit; the name is misleading).
- `startDate` → `date`; `endDate` removed.
- `limitAmount` is a **String** in Room but a **Double** in the API.

### Expense – ❌ shape differs most
| | Columns |
|---|---|
| Room `expenses` | id, userId, **accountId, transactionId**, name, **principal**, tenure (Int, non-null), **monthlyEmi**, **startDate, endDate, nextDueDate, remainingBalance**, **monthsPaid**, **active**, createdAt, updatedAt, accountName, syncStatus, isDeleted |
| Backend request | type (`expense`/`subscription`), name, description, amount, date, startDate, categoryId, accountId, tenure, monthsPaid |
| Backend response | id, userId, accountId, categoryId, transactionId, name, description, amount, type, date, principal, tenure, monthlyEmi, startDate, endDate, nextDueDate, remainingBalance, monthsPaid, active, createdAt, updatedAt |
| Target | id, userId, date, amount, categoryId, tenure, paid, name |

Gaps:
- **No `categoryId` in Room** (the backend has it).
- `tenure` is a non-null Int defaulting to `0` for subscriptions → must become **nullable**, `null` = every month.
- `monthlyEmi` → `amount`. Today the same value is copied into `principal`, `monthlyEmi` and
  `remainingBalance` on save, and the server may return it only as `amount` (fixed in the mapper last change).
- `monthsPaid` → `paid`; `startDate` → `date`.
- Remove `principal`, `endDate`, `nextDueDate`, `remainingBalance`, `active` (all derivable from
  date + tenure + paid), `transactionId`, `accountId`.
- The `type` (`expense`/`subscription`) the app sends becomes unnecessary: `tenure == null` already says it.
- The UI splits EMI vs subscription with `tenure > 0` (`CashflowActivity`); it should become `tenure != null`.

---

## 4. Monthly calculation (prev / this / next)

For a month `M`:

| Figure | Formula |
|---|---|
| Planned income | Σ `budget.amount` where `budget.date <= M` |
| Planned outgoing | Σ `expense.amount` where `expense.date <= M` and (`tenure == null` or `paidAt(M) < tenure`) |
| Actual income | Σ `transaction.amount` in `M` whose category type is `INCOME` |
| Actual outgoing | Σ `transaction.amount` in `M` whose category type is `EXPENSE` |
| Next month forecast | planned income − planned outgoing (later: + average of variable spend) |

`paidAt(M) = paid + monthsBetween(month the row was saved, M)`. This needs to know which month
`paid` was counted in; `updatedAt` can serve, or keep `paid` updated as EMIs are paid.

---

## 5. Other cleanups requested
- **Remove WEEKLY / YEARLY**: only `OnboardingScreen.kt` (2 places) offers them; `periodType` flows
  through ~12 files (entity, DTO, mapper, repository, use cases, sync worker, cashflow, onboarding).
- **Dates**: one shared picker, `presentation/component/ExpentDatePicker.kt`, used by onboarding (4),
  cashflow (1), dashboard (1). It currently returns a full date. See open question 1.

## 6. Migration notes
- Room is at `version = 3` with `fallbackToDestructiveMigration()`, so changing the schema just wipes
  local data and it re-downloads after login. Rows still in `pending_sync` would be lost, so do it in
  a release where users have synced.
- App and backend must change together (or the app maps old ↔ new fields during the transition).
  The order: backend accepts the new fields → app switches → backend drops the old ones.

## 7. Decisions
1. **Date** = **day of the month** (1-31) for Budget and Expense. Transactions keep a full date
   (a transaction is a real event and the month matters for prev / this month figures).
2. **Backend changes later.** The app converts at the API boundary (`data/mapper/EntityMappers.kt`).
3. **Transaction `type` stays** (type exists on Category, Account and Transaction).
4. **Expense has no account** (`accountId` / `accountName` removed); it has a category (EXPENSE type).
5. **Budget has no end date.** An income that stops is deleted.

## 8. Implemented on the app side (Room v6)

| Entity | Room columns now |
|---|---|
| `budgets` | id, userId, categoryId, **amount** (Double), **date** (day 1-31), categoryName*, createdAt, updatedAt, syncStatus, isDeleted |
| `expenses` | id, userId, name, **amount** (Double), **date** (day 1-31), **categoryId**, **tenure** (Int?, null = every month), **paid**, createdAt, updatedAt, syncStatus, isDeleted |
| `transactions` | id, userId, accountId, categoryId, type, **amount** (Double), **date** (ISO timestamp), categoryName*, accountName*, createdAt, updatedAt, syncStatus, isDeleted |

\* display cache only. `createdAt` / `updatedAt` stay as sync metadata (also the reference month
for `paid` later).

- UI reads domain models `domain/model/Budget` and `domain/model/Expense`, not API DTOs.
- Weekly / yearly removed from onboarding; everything is monthly.
- Budget and Expense pickers use `presentation/component/DayOfMonthPicker` ("5th of every month").
- Sync re-reads the row from Room when it pushes, so offline edits / deletes before the first sync
  are respected and budgets saved against a not-yet-synced category get its server id.

### Mapping to the current API (until the backend changes)

| App | Sent to API | Read from API |
|---|---|---|
| Budget.amount | `limitAmount` | `limitAmount` |
| Budget.date | `startDate` = that day in the current month; `periodType = "MONTHLY"` | day of `startDate` |
| Expense.amount | `amount` | `monthlyEmi`, else `amount` |
| Expense.date | `startDate` = that day in the current month | day of `startDate` / `date` |
| Expense.tenure | `tenure`; `type` = `subscription` when null, else `expense` | `tenure` (0 → null) |
| Expense.paid | `monthsPaid` | `monthsPaid` |

### Backend to-do (later)
- Budget: `amount`, `date` (int day), drop `periodType` / `limitAmount`. (`endDate` already removed from schema and API.)
- Expense: `amount`, `date` (int day), nullable `tenure`, `paid`, keep `categoryId`, drop `accountId`;
  drop `type`. The Prisma `Emi` model never had `principal`, `monthlyEmi`, `remainingBalance`, `nextDueDate`, `endDate` or `active`; the app DTO lists fields the server does not send.
- Then delete the mapping table above from `EntityMappers.kt`.

### Transaction API (app and backend changed together)
- `POST/PUT /transactions` body: `{ type, amount, date, accountId, categoryId }`; `date` is RFC 3339 (or yyyy-MM-dd).
  Before this, the app sent `transactionDate`, which the backend ignored, so every transaction was saved with today's date.
- Response: `{ id, userId, accountId, categoryId, amount, type, date, createdAt, updatedAt }`; `timestamp`, `notes`,
  `description` removed. Prisma `Transaction.notes` dropped.
- `POST /parse-transaction` data: `{ type, amount, date, category_id, category_name, account_id, account_name }`;
  the Gemini prompt asks only for these.
- Transactions show their category name as the title (no note / merchant).
