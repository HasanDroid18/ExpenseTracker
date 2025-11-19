# Expense Tracker App

## 📌 Features

### 🔐 User Authentication
- Secure login and token-based authentication.

### 🏠 Home Dashboard
- View a summary of expenses and income.
- See a list of recent transactions.

### ➕ Add Transaction
- Add new income or expense entries with details.

### ✏️ Edit & ❌ Delete Transactions
- Update or remove existing transactions easily.

### ⚙️ Settings
- **About Us Dialog**: View app information in a modern alert dialog.  
- **Language Selection**: Choose app language via a Material Design bottom sheet.  
- **Logout**: Securely log out of your account.  
- **Biometric Security Toggle**: Enable/disable fingerprint/PIN lock.

### 🔒 App Lock
- Biometric & device credential authentication (fingerprint, face, PIN)
- Encrypted fallback 4-digit PIN
- Toggle control in Settings

### 📦 Data Caching
- Home and History data cached to reduce redundant network calls.

### 💱 Currency Converter
- Convert between USD and LBP with live rate

### 🎨 Material Design UI
- Clean, modern, and responsive user interface.

### ⚠️ Error Handling
- User-friendly error messages and loading indicators.

### 🌐 Localization
- English & Arabic language switching.

## 📚 Extended Documentation
- Full Feature Catalog: `APP_FEATURES_DOCUMENTATION.md`
- Biometric & PIN Lock Details: `BIOMETRIC_LOCK_DOCUMENTATION.md`
- Setup & Testing Guide: `SETUP_GUIDE.md`
- Implementation Summary: `IMPLEMENTATION_SUMMARY.md`

## 🚀 Tech Stack
Kotlin, MVVM, Hilt, Retrofit, Coroutines, DataStore, EncryptedSharedPreferences, MPAndroidChart, Material Components.

## 🛠 Build Commands
```bash
./gradlew assembleDebug
./gradlew clean
```
