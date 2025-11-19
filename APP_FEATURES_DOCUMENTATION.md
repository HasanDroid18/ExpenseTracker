# Expense Tracker Application - Full Feature Documentation

Last Updated: November 20, 2025
Version: 1.0
Minimum Android Version: API 24
Target Android Version: API 36

## 1. Overview
The Expense Tracker app helps users record, visualize, and manage their financial transactions (income and expenses), view historical data, convert currency, and secure access using biometric/PIN authentication. Built with Kotlin, MVVM architecture, Hilt DI, Jetpack components, DataStore, Retrofit, and Material Design.

## 2. High-Level Feature List
- User Authentication (Signup/Login + persistent token)
- Secure App Lock (Biometric / Device Credential / App PIN)
- Home Dashboard (Summary + Bar Chart + Quick Add Transaction)
- Add Transaction (Income/Expense with category selection)
- Transaction History (List, classification, deletion)
- Currency Converter (USD ↔ LBP real-time rate usage)
- Settings (Language selection, About dialog, Logout, Biometric toggle)
- Multi-language support (English + Arabic with manual locale application)
- Data Caching (Home summary + History transactions)
- Error Handling + Loading Indicators
- Clean Material Design UI Components

## 3. Technology Stack
| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| DI | Hilt (dagger.hilt.android) |
| Network | Retrofit + OkHttp (Logging Interceptor) |
| Async | Kotlin Coroutines |
| Persistence | DataStore (Preferences) + EncryptedSharedPreferences (for PIN) |
| UI | Fragments, Activities, ViewBinding, Material Components |
| Charts | MPAndroidChart |
| Security | AndroidX Biometric, Android Security Crypto |

## 4. Architecture
- Pattern: MVVM (ViewModel + Repository + UI Observers)
- Reactive Data: LiveData + Flow (DataStore)
- Separation of Concerns: Each screen has its own ViewModel & repository
- Caching Flags: Used to avoid redundant network calls (Home, History)

```
UI (Activity/Fragment)
    ↓ observes
ViewModel (Business State)
    ↓ calls
Repository (Network/Data Layer)
    ↓ uses
Retrofit/Datastore/EncryptedPrefs
```

## 5. Authentication & User Session
### Files
- `auth/Login/LoginFragment.kt`
- `auth/SignUp/SignupFragment.kt`
- `auth/AuthActivity.kt`
- `auth/SplashScreen.kt`
- `auth/UserDataStore.kt`

### Flow
1. User opens app → SplashScreen shows (3s delay)
2. Splash checks DataStore token:
   - Token present → Navigate (and possibly authenticate via biometric)
   - Token absent → AuthActivity (Login/Signup fragments)
3. Login success stores token + user profile (DataStore)
4. Signup success → Navigate back to Login
5. Logout clears DataStore and sends user back to SplashScreen

### DataStore Keys
- USER_NAME
- USER_EMAIL
- USER_TOKEN

### Security
- Token persisted in DataStore (NOT plain SharedPreferences)
- App lock layer triggered only when logged in

## 6. Biometric & App Lock
See `BIOMETRIC_LOCK_DOCUMENTATION.md` for deep dive.

### Files
- `auth/biometric/BiometricAuthManager.kt`
- `auth/biometric/PinLockActivity.kt`
- `auth/biometric/PinManager.kt`
- `auth/biometric/BiometricPreferenceManager.kt`
- `auth/SplashScreen.kt` (integration point)
- `AppScreens/Settings/SettingsFragment.kt` (toggle)

### Modes
| Mode | Trigger | Result |
|------|---------|--------|
| Biometric Enabled | App launch (logged in) | Prompt (Fingerprint/Face/PIN) |
| Biometric Disabled | App launch | Direct to MainActivity |
| No Biometric | Prompt fallback | PIN dialog setup/verify |

### Fallback PIN
- 4-digit numeric PIN
- Stored encrypted via EncryptedSharedPreferences
- Setup: Enter twice for confirmation
- **Security Features:**
  - Progressive throttling: 2-second delay after 3 failed attempts (increases per attempt)
  - 30-second lockout after 5 failed attempts
  - 5-minute lockout after 10 failed attempts
  - Persistent attempt tracking (survives app restarts)
  - Visual countdown during lockout
  - Number pad disabled during delays/lockout
  - Attempt counter resets on successful authentication

## 7. Home Dashboard
### Files
- `AppScreens/Home/HomeFragment.kt`
- `AppScreens/Home/HomeViewModel.kt`
- `AppScreens/Home/HomeRepository.kt`
- `AppScreens/Home/SummaryResponse.kt`
- `AppScreens/Home/MonthlySummaryResponse.kt`

### Features
- Displays username (from DataStore)
- Shows total income, expenses, balance
- Bar chart visualizing Income vs Expenses vs Balance
- Add Transaction shortcut
- Automatic refresh on resume
- Pull-to-refresh (SwipeRefreshLayout)
- Data caching (initial load vs manual refresh tracking)

### Chart Behavior
- Uses MPAndroidChart BarChart
- Axis labels: Income, Expenses, Balance
- Colors: Green (Income), Red (Expenses), Blue (Balance)
- Animates vertically (600ms)
- Handles empty/no data gracefully

### Edge Handling
- Missing numeric prefixes forced to `$` formatting
- Invalid or blank currency values default to `$0.00`

## 8. Add Transaction
### Files
- `AppScreens/Home/AddTransaction/AddTransActivity.kt`
- `AppScreens/Home/AddTransaction/AddTransViewModel.kt`
- `AppScreens/Home/AddTransaction/TransactionRequest.kt`

### Features
- Add income or expense with:
  - Title
  - Amount
  - Category (ChipGroup selection)
- Validation (non-empty, numeric amount)
- Displays loading spinner while saving
- Success: Toast + closes activity
- Failure: Toast with API/server error

### Categories
(Defined as chips in layout): Income, Food, Transport, Shopping, Bills, etc.

## 9. Transaction History
### Files
- `AppScreens/History/HistoryFragment.kt`
- `AppScreens/History/HistoryViewModel.kt`
- `AppScreens/History/HistoryRepository.kt`
- `AppScreens/History/TransactionResponse.kt`
- `AppScreens/History/TransactionAdapter.kt`

### Features
- RecyclerView list of transactions
- Dynamic icon & color based on category (Income → Up Arrow Green, Expense → Down Arrow Red)
- Amount formatted with + or - prefix
- Empty state placeholder item when list empty
- Deletion (inline button) triggers repository call then refreshes list
- Caching window (5 minutes) before auto-refresh

### Resilience
- Errors surfaced via Toast
- UI dimming (alpha) during loading state

## 10. Currency Converter
### Files
- `AppScreens/Converter/ConverterFragment.kt`
- `AppScreens/Converter/ConverterViewModel.kt`
- `AppScreens/Converter/ConverterRepository.kt`

### Features
- Convert between USD and LBP
- Fetches exchange rate asynchronously on init & manual refresh
- Modes:
  - USD → LBP (amount * rate)
  - LBP → USD (amount / rate)
- Shows current rate hint
- Disables Convert button while loading
- Input validation with inline error
- Auto keyboard dismissal on action done / background tap

### Edge Cases
- Rate unavailable → Error message
- Divide-by-zero protected when rate == 0.0

## 11. Settings
### Files
- `AppScreens/Settings/SettingsFragment.kt`
- `AppScreens/Settings/SettingsViewModel.kt`
- `AppScreens/Settings/SettingsRepository.kt`

### Features
- Profile header (username + email)
- Language selection bottom sheet (English / Arabic)
- About Us dialog with app info
- Biometric Security toggle (SwitchMaterial)
- Logout button (confirmation dialog)

### Localization
- Locale manually applied using `Configuration.setLocale`
- Forces LTR layout regardless of language

### Logout Flow
1. User taps Logout
2. Confirmation dialog
3. `SettingsViewModel.logout()` calls repository
4. On success → Clear token → Navigate to Splash → Activity finished

## 12. Data Persistence & Caching
### Mechanisms
| Mechanism | Purpose |
|-----------|---------|
| DataStore (Preferences) | User credentials & profile |
| In-Memory Flags | Track loaded/cached data |
| EncryptedSharedPreferences | Secure PIN storage |

### Caching Strategy
- Home & History store time-of-last-fetch
- If elapsed > threshold (5 minutes for history), data refreshed
- Manual refresh bypasses cache (SwipeRefreshLayout / onResume)

## 13. Error Handling & UX Feedback
| Context | Mechanism |
|---------|-----------|
| Network Failure | Toast messages |
| Loading States | ProgressBar / alpha dimming / swipe spinner |
| Empty Lists | Placeholder item view |
| Validation | Early return with toast or field error |
| Authentication Failure | Toast + app exit |

## 13. Security Model
| Vector | Mitigation |
|--------|------------|
| Unauthorized Access | Biometric / Device Credential / PIN gate |
| Plain-text credential storage | DataStore + encryption for PIN |
| Stale sessions | Logout clears DataStore |
| Brute force on PIN | ✅ Progressive throttling + temporary lockout (30s/5min) |
| Rapid PIN attempts | ✅ Attempt tracking with escalating delays |
| Sensitive token exposure | Not logged in once DataStore cleared |

## 15. Performance Considerations
- Minimal recomputation: Observers only update UI when LiveData changes
- Chart only re-rendered on summary updates
- History list only rebuilt when transactions change or deletion occurs
- Converter preloads rate once and reuses

## 16. Extensibility Points
| Future Feature | Suggested Approach |
|---------------|--------------------|
| Budget Planning | New module + repository + chart view |
| Recurring Transactions | Add recurrence field to TransactionRequest |
| Export Data (CSV/PDF) | Use WorkManager + file provider |
| Dark Mode | Add theme toggling using DataStore preference |
| Notifications | Use AlarmManager / WorkManager for reminders |
| Multi-Currency | Extend Converter to support dynamic currency list |
| Analytics Trends | Add LineChart / PieChart (MPAndroidChart) |

## 17. App Navigation Structure
```
SplashScreen → (AuthActivity: LoginFragment ↔ SignupFragment)
            → MainActivity (Bottom Navigation)
                    ├─ HomeFragment
                    ├─ HistoryFragment
                    ├─ ConverterFragment
                    └─ SettingsFragment
AddTransActivity (Modal/activity launched from Home)
PinLockActivity (Fallback auth when needed)
```

## 18. Dependencies Summary
```
implementation("androidx.biometric:biometric:1.2.0-alpha05")
implementation("androidx.security:security-crypto:1.1.0-alpha06")
implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1")
implementation("androidx.datastore:datastore-preferences:1.1.7")
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
implementation("com.airbnb.android:lottie:6.6.6")
implementation("com.google.dagger:hilt-android:2.48")
```

## 19. Testing Matrix
| Scenario | Expected Result |
|----------|-----------------|
| Launch logged-in + biometric enabled | Prompt shows → Success navigates to Main |
| Launch logged-in + biometric disabled | Direct navigation to Main |
| Launch without token | Navigate to AuthActivity |
| Add transaction valid data | Success toast + return |
| Add transaction invalid data | Validation toast |
| Delete transaction | List updates, removed item gone |
| History empty | Empty state item rendered |
| Converter invalid number | Input error message |
| Toggle biometric off | Next launch skips authentication |
| PIN fallback setup | Double entry required, saved encrypted |
| PIN incorrect | Error + retry allowed |
| Logout | Token cleared + back to Splash |
| Language change | UI restarts with new locale |

## 20. Known Limitations
- No pagination for large transaction sets
- Exchange rate source unspecified (assumes repository provides data)
- No offline-first caching for network failure
- Locale switching uses deprecated configuration updates (acceptable for MVP; could migrate to AppCompatDelegate APIs)

## 21. Suggested Improvements (Roadmap)
1. Migrate locale handling to modern APIs
2. Add retry/backoff to network layer
3. Implement dark mode
4. Add biometric lock timeout (auto-unlock window)
5. Add transaction editing feature
6. Add tests (JUnit + Espresso + Robolectric) for critical flows
7. Secure token using encrypted storage (if higher sensitivity required)
8. Introduce pagination & filtering for History
9. Persist chart historical data for analytics
10. Add accessibility improvements (content descriptions, TalkBack labels)

## 22. Code Quality Notes
- ViewBinding used for safe view access
- LiveData and Flow separation ensures reactive UI
- Repositories encapsulate data operations
- Minimal logic inside Fragments/Activities (delegated to ViewModels)
- Error messages user-friendly, low technical leakage

## 23. Quick Start Commands
```bash
# Build
./gradlew assembleDebug

# Run tests (if added later)
./gradlew test

# Clean
./gradlew clean
```

## 24. Support & Maintenance
For maintenance:
- Update biometric library to stable when available
- Periodically review security-crypto dependency for CVEs
- Monitor network layer for timeout/retry configuration

## 25. Glossary
| Term | Meaning |
|------|---------|
| PIN | Personal Identification Number (app local fallback) |
| BiometricPrompt | Android system UI for secure biometric/device credential auth |
| DataStore | Jetpack library for typed, async key-value storage |
| MVVM | Model-View-ViewModel architectural pattern |
| EncryptedSharedPreferences | AndroidX secure preferences wrapper |
| Repository | Class handling data operations (network/persistence) |

---
This document provides a comprehensive view of the Expense Tracker app capabilities, structure, and future evolution paths. For deeper security details see `BIOMETRIC_LOCK_DOCUMENTATION.md`; for setup steps see `SETUP_GUIDE.md`.

