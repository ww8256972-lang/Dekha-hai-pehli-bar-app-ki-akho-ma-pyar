# ASLAM RECORD — Native Android Shop Management App

**ASLAM RECORD** is a modern, native Android application built with Kotlin, Jetpack Compose, Material 3, and Room Database. Designed specifically for retail shopkeepers and wholesalers to manage inventory, sales, purchases, customer credit ledgers (Khata / Udhar), official receipts, operating expenses, payment reminders via WorkManager and WhatsApp, and real-time financial reporting.

---

## 🌟 Key Features

1. **Product & Inventory Management**
   - Add, edit, search, and archive products.
   - Track purchase cost, selling price, current stock, minimum stock alert, and units (Pieces, Packets, Boxes, Kg, Liter, Meter, Dozen).
   - Auditable stock movement history log (`PURCHASE_IN`, `SALE_OUT`, `ADJUSTMENT_ADD`, `ADJUSTMENT_DEDUCT`, `DAMAGE_LOSS`).
   - Real-time stock valuation at cost and retail value.

2. **Sales & Digital Invoices**
   - Fast item billing with live quantity steppers, subtotal, discounts, and tax calculation.
   - Support for Walk-in cash customers as well as registered Khata accounts.
   - Flexible payment modes: Cash, UPI / Online, Bank Transfer, Cheque, and Credit Khata.
   - Generates official receipts with instant WhatsApp sharing and Android share sheet support.

3. **Customer & Supplier Khata (Udhar / Credit Ledger)**
   - Maintain customer profiles with mobile numbers, credit limits, and outstanding debt balances.
   - Record customer repayments with instant ledger deduction.
   - Direct phone calling via Android dialer integration.
   - Official WhatsApp payment reminders formatted with shop branding.
   - Supplier payables tracking and purchase records.

4. **Expenses & Financial Reports**
   - Profit & Loss statement: Gross Revenue − Cost of Goods Sold (COGS) = **Gross Profit**.
   - Gross Profit − Operating Expenses = **Net In-Pocket Profit**.
   - Operating expense logging by category (Rent, Electricity, Salaries, Transport, Tea & Snacks, Maintenance).
   - Shareable/Exportable text & PDF audit reports.

5. **Local Due Date Reminders (WorkManager)**
   - Scheduled periodic background workers (`PaymentReminderWorker`) to alert merchant about overdue customer payments.
   - Resilient against device reboots with `RECEIVE_BOOT_COMPLETED` permission.

6. **Official WhatsApp Business Integration**
   - Dual-mode architecture:
     - **Direct Native App Messaging**: Zero-configuration mode using WhatsApp app URI schemes.
     - **Meta WhatsApp Cloud API**: Supports official Cloud API phone number ID and access token configured via Settings.

7. **ASLAM AI Advisor**
   - Intelligent business copilot that analyzes profit margins, customer credit risks, dead stock, and reorder warnings.
   - Connects to Gemini API when `GEMINI_API_KEY` is provided in environment secrets, or gracefully operates using the built-in on-device financial accounting intelligence engine.

---

## 🏗️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose (100% Kotlin), Material Design 3 (M3)
- **Architecture**: MVVM + Clean Architecture (Repository Pattern)
- **Local Persistence**: Room Database 2.7.0 with Kotlin Symbol Processing (KSP) and TypeConverters
- **Background Tasks**: AndroidX WorkManager 2.10.0
- **Networking**: Retrofit 2.12.0 + OkHttp + Moshi Kotlin
- **Asynchronous Flow**: Kotlin Coroutines & StateFlow
- **CI/CD**: GitHub Actions workflow building and validating debug APK artifacts

---

## 🚀 Building Locally

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API Level 36 / 35 / 34, Min SDK 24)
- Gradle 9.3.1 (included via Gradle Wrapper)

### Build Commands

```bash
# Check Gradle Wrapper
./gradlew --version

# Run Unit Tests & Lint
./gradlew testDebugUnitTest lintDebug

# Build Debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤖 GitHub Actions Workflow

The repository includes a production-grade CI pipeline configured at `.github/workflows/android.yml`:

- **Triggers**: Pushes to `main`, Pull Requests to `main`, and manual execution (`workflow_dispatch`).
- **Steps**:
  1. Validates the Gradle Wrapper checksum.
  2. Sets up JDK 17 (Temurin) and Android SDK licenses.
  3. Executes `./gradlew --no-daemon testDebugUnitTest lintDebug`.
  4. Builds the debug APK via `./gradlew --no-daemon assembleDebug`.
  5. Verifies file integrity and uploads `app-debug.apk` as a downloadable artifact (`aslam-record-debug-apk`).

---

## 🔒 Configuration & Secrets

### Secrets in AI Studio
Configure your keys in the **Secrets panel in AI Studio** or `.env`:
- `GEMINI_API_KEY`: *(Optional)* Your Google Gemini API key for cloud generative AI features.

### WhatsApp Cloud API (Optional)
If using the official Meta WhatsApp Business Cloud API rather than direct app sharing, enter your Cloud Phone ID and Access Token in the app's **Settings Screen**.
