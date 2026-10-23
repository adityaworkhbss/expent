# Expent: Google Play listing kit

Everything to fill in on Play Console for the first release, plus the fixes needed before submitting.
The character limits are Play Console's.

---

## ⚠️ Fix before submitting

These either break Play policy or look unfinished to reviewers:

1. **Test login is on in the production build.** `app/build.gradle.kts` sets `USE_TEST_LOGIN = true` in
   the `prod` flavor, so release builds show a login that skips Google Sign-In. Set it to `false` for
   `prod`, and check the backend's `/auth/test-login` is disabled or protected in production.
2. **Account deletion is required.** Google Sign-In creates an account on your server, and Play policy
   then requires:
   - ✅ a way to delete the account and its data **inside the app**. Profile → Delete account calls
     `DELETE /auth/account`, which removes the user and all their data, and the app wipes local data.
   - ⏳ a **web link** where users can request deletion without the app. This link goes in the Data
     safety form. A simple page with a support email and the steps is enough.
3. **Privacy policy URL.** Play requires a public privacy policy page.
   - ✅ Written: [legal/PRIVACY_POLICY.md](legal/PRIVACY_POLICY.md) and
     [legal/TERMS_OF_SERVICE.md](legal/TERMS_OF_SERVICE.md). The in-app sheet shows a short summary
     of both (remote config key `auth.legalSheet.content`).
   - ⏳ Fill in `[DEVELOPER NAME]`, `[SUPPORT EMAIL]` and the governing-law placeholders, host both
     pages (e.g. GitHub Pages or a Notion page), and paste the privacy URL into Play Console. The
     privacy policy's "Delete without the app" section can also serve as the account-deletion web
     link (#2).
4. **Version code.** `versionCode = 1` / `versionName = "1.0.1"`. Fine for the first upload, but
   increase `versionCode` for every upload after that.
5. **Release build hygiene.**
   - ✅ R8 is on (`isMinifyEnabled` + `isShrinkResources`, rules in `app/proguard-rules.pro`). Test a
     release build end to end once: sign-in, sync, AI entry, push.
   - Upload an **Android App Bundle (.aab)** signed with your upload key.
   - Enroll in **Play App Signing**.
6. **Currency.** Amounts display as `$`. If you target India first, either show `₹` or say "USD" in
   the screenshots and listing so users aren't surprised.
7. **Unfinished screen.** The Analysis screen is empty. Make sure nothing in the UI links to it.

---

## Store listing

**App name** (max 30)
```
Expent: AI Expense Tracker
```

**Short description** (max 80)
```
Type a sentence, track your money. Offline expense, EMI & subscription tracker.
```

**Full description** (max 4,000)
```
Expent is the expense tracker that keeps up with you. Just type what happened, like "Spent $25 on pizza yesterday using cash", and Expent's AI fills in the amount, category, account and date for you.

TRACK SPENDING IN SECONDS
• AI entry: describe a transaction in plain words and save it in one tap
• Manual entry with categories, accounts and dates when you want full control
• Add an expense or income straight from your home screen with the Expent widget

NEVER MISS A DUE DATE
• Track EMIs and loans with repayment progress
• Keep all your subscriptions in one place
• Get reminders before bills, EMIs and subscriptions are due, and mark them paid in one tap
• Plan around your salary day and other regular income

SEE WHERE YOUR MONEY GOES
• Dashboard with your balance, income and expenses at a glance
• Cashflow view of everything coming in and going out every month
• Full transaction history with search and filters by type and category

WORKS OFFLINE
Everything you add is saved on your phone first and syncs in the background when you're back online, so you can log expenses anywhere.

MADE FOR THE WAY YOU PAY
• Set up your own categories for spending and income
• Separate "pay now" accounts (bank, UPI, cash) from "pay later" ones (credit cards, BNPL)
• Quick four-step setup gets you tracking in about two minutes

PRIVATE AND SECURE
• Sign in with your Google account, no new password to remember
• Your session data is stored in encrypted storage on your device
• Your data is used only to run Expent. We don't sell it.

Have an idea or found a bug? Tell us from Profile → Help us improve. We read every note.
```

**Category:** Finance
**Tags** (pick up to 5 in Play Console): Budgeting, Expense tracker, Personal finance, Bill reminders, Money manager
**Contact email:** your support address (required, shown publicly)
**Website:** optional
**Privacy policy URL:** required (see fix #3)

---

## Graphics checklist

| Asset | Spec | Suggested content |
|---|---|---|
| App icon | 512 × 512 PNG, 32-bit, no transparency issues | Current launcher icon at high resolution |
| Feature graphic | 1024 × 500 PNG/JPG | Logo + "Type it. Track it." + a phone showing the AI entry sheet |
| Phone screenshots | 2–8, 16:9 or 9:16, min 320 px | 1. AI entry ("Spent $25 on pizza…" → saved) · 2. Dashboard with balance + reminders · 3. Cashflow (EMIs & subscriptions) · 4. Transaction history with filters · 5. Onboarding · 6. Home-screen widget |
| Tablet screenshots | optional | Skip for v1 unless you test tablet layouts |

Tip: add a one-line caption to each screenshot, e.g. "Just type it", "Never miss an EMI",
"Works offline".

---

## App content (Policy section)

### Data safety form

Answers based on what the app and backend do today. Check them again if anything changes.

**Does your app collect or share user data?** Yes, collected. Not shared with third parties for
their own use. Firebase and Google Gemini act as service providers processing data on your behalf.

**Is all data encrypted in transit?** Yes (HTTPS everywhere).
**Can users request deletion?** Must be **Yes** before launch (see fix #2).

| Data type | Collected | Why | Optional? |
|---|---|---|---|
| Personal info → Name | Yes | Account management (from Google Sign-In) | Required |
| Personal info → Email address | Yes | Account management, sign-in | Required |
| Personal info → User IDs | Yes | Account management | Required |
| Financial info → Other financial info (transactions, accounts, budgets, EMIs, subscriptions) | Yes | App functionality | Required |
| App activity → Other user-generated content (feedback text + rating) | Yes | Analytics / product improvement | Optional |
| App info and performance → Crash logs | Yes | Crashlytics: diagnostics | Required |
| App info and performance → Diagnostics | Yes | Crashlytics: diagnostics | Required |
| App activity → App interactions | Yes | Firebase Analytics | Required |
| Device or other IDs | Yes | FCM push token: notifications. Analytics app-instance ID | Required |

Text typed into **AI entry** is sent to Google Gemini to parse the transaction. List it under
Financial info → App functionality, and mention Gemini in the privacy policy.

### Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Sync data with Expent's servers |
| `POST_NOTIFICATIONS` (Android 13+) | Bill/EMI/subscription reminders and announcements; the user is asked at runtime and can decline |

No sensitive permissions (SMS, contacts, location, storage), so no extra declarations are needed.

### Other declarations
- **Ads:** No ads.
- **App access:** The app requires sign-in. Give reviewers working access, either:
  - a Google test account with instructions, or
  - a reviewer-only login that is disabled for real users.

  Don't rely on the debug test login (see fix #1).
- **Content rating (IARC questionnaire):** Utility/productivity app. No violence, sexual content,
  gambling, drugs, user-to-user chat or location sharing. It should come out **Everyone / PEGI 3**.
- **Target audience:** 18+ (handles personal finances). Not designed for children.
- **Financial features declaration:** Choose personal finance management / budgeting. Expent doesn't
  offer loans, banking, payments, crypto or investment services.
- **Government app / news app / health:** No.

---

## Release notes (v1.0.1, first release)

```
Welcome to Expent!
• Add expenses by just typing a sentence: AI does the rest
• Track EMIs, loans and subscriptions with due-date reminders
• Dashboard, cashflow and full transaction history
• Works offline and syncs in the background
• Home-screen widget for quick entries
```

---

## Suggested rollout

1. **Internal testing** (up to 100 testers, available within minutes): check Google Sign-In with the
   release signing key's SHA-1/SHA-256 registered in Firebase and Google Cloud, and check push
   notifications.
2. **Closed testing:** new personal developer accounts must run a closed test with **at least 12
   testers for 14 days** before production access is granted.
3. **Production**, with a staged rollout (e.g. 20% → 50% → 100%) while you watch Crashlytics.

Before step 1, register the **release / Play App Signing SHA-1** in the Firebase project and the
Google Cloud OAuth client. Without it, Google Sign-In fails in Play-installed builds even though it
works in debug.
