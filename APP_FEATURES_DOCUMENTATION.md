# Expense Tracker Application - Full Feature Documentation

Last Updated: November 20, 2025
Version: 1.0
Minimum Android Version: API 24
Target Android Version: API 36

## 1. Overview
The Expense Tracker app helps users record, visualize, and manage their financial transactions (income and expenses), view historical data, convert currency, and secure access using biometric/PIN authentication. Built with Kotlin, MVVM architecture, Hilt DI, Jetpack components, DataStore, Retrofit, and Material Design.

## 2. High-Level Feature List
- User Authentication (Signup/Login + persistent token)
- Secure App Lock (Biometric / Device Credential / App PIN with anti-brute-force protection)
- Home Dashboard (Summary + Bar Chart + Quick Add Transaction)
- Add Transaction (Income/Expense with category selection)
- Transaction History (List, classification, deletion)
- Currency Converter (USD ↔ LBP real-time rate usage)
- Settings (Language selection, About dialog, Logout, Biometric toggle)
- Multi-language support (English + Arabic with manual locale application)
- Data Caching (Home summary + History transactions)
- Offline Mode Handling (Clear dialogs with retry mechanism)
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

### Overview
The app implements a comprehensive security layer using biometric authentication (fingerprint, face, device credentials) with PIN fallback. Security can be toggled in Settings, and authentication is required at app launch when enabled.

### Files & Components
| File | Purpose |
|------|---------|
| `auth/biometric/BiometricAuthManager.kt` | Handles biometric authentication using AndroidX BiometricPrompt |
| `auth/biometric/BiometricPreferenceManager.kt` | Manages security on/off preference |
| `auth/biometric/PinManager.kt` | Manages encrypted PIN storage and verification |
| `auth/biometric/PinLockActivity.kt` | UI for PIN setup and verification with security features |
| `auth/biometric/PinAttemptManager.kt` | Tracks failed attempts and enforces throttling/lockout |
| `auth/SplashScreen.kt` | Integration point - checks and triggers authentication |
| `AppScreens/Settings/SettingsFragment.kt` | Security toggle with authentication requirement |

### Authentication Flow
```
App Launch (Logged In)
    ↓
Check: Is security enabled?
    ↓
   YES → Request Authentication
    ↓
Check: Biometric available?
    ↓
    ├─ YES → Show BiometricPrompt
    │         ├─ Success → Navigate to Main
    │         ├─ Error/Cancel → Close app
    │         └─ Lockout → Fallback to PIN
    │
    └─ NO → Check: PIN set up?
              ├─ YES → Show PIN screen
              │        ├─ Correct → Navigate to Main
              │        └─ Wrong → Apply throttling/lockout
              │
              └─ NO → Show setup dialog → Setup PIN or Close
```

### Biometric Implementation

#### BiometricAuthManager
Uses `androidx.biometric.BiometricPrompt` to provide system-level authentication:
- **Authenticators**: `BIOMETRIC_STRONG` (fingerprint, face) + `DEVICE_CREDENTIAL` (device PIN/pattern)
- **Status Check**: Detects if biometric hardware exists and is enrolled
- **Callbacks**: Success, error (with error codes), and failure handling
- **Cancellation**: Allows canceling ongoing authentication

**Key Methods:**
```kotlin
fun checkBiometricAvailability(): BiometricStatus
fun authenticate(onSuccess, onError, onFailure)
fun cancelAuthentication()
```

#### BiometricStatus Enum
- `AVAILABLE` - Biometrics enrolled and ready
- `NO_HARDWARE` - Device has no biometric hardware
- `HARDWARE_UNAVAILABLE` - Hardware temporarily unavailable
- `NONE_ENROLLED` - No fingerprint/face enrolled
- `SECURITY_UPDATE_REQUIRED` - System update needed
- `UNSUPPORTED` - Feature not supported
- `UNKNOWN` - Unknown status

### PIN Fallback System

#### PinManager
Manages encrypted PIN storage using `EncryptedSharedPreferences`:
- **Storage**: PIN hashed and encrypted at rest
- **Validation**: Constant-time comparison to prevent timing attacks
- **Setup Check**: `isPinSet()` verifies PIN configuration

**Security:**
- Uses Android Security Crypto library
- MasterKey with AES256_GCM encryption
- Stored in isolated EncryptedSharedPreferences

#### PinLockActivity
Full-featured PIN authentication UI:
- **Setup Mode**: First-time PIN creation with confirmation
- **Verify Mode**: Authenticate using existing PIN
- **Number Pad**: 0-9 buttons + delete + cancel
- **Visual Feedback**: 4 dots showing PIN entry progress
- **Security Integration**: Works with PinAttemptManager for throttling

**User Experience:**
- Smooth animations
- Clear instructions
- Real-time feedback
- Cancel closes entire app (security by design)

#### PinAttemptManager
Implements anti-brute-force protection:

**Throttling Levels:**
| Failed Attempts | Action |
|----------------|--------|
| 1-2 | Immediate retry allowed |
| 3 | 2-second delay before next attempt |
| 4 | 4-second delay (doubles) |
| 5 | 30-second lockout with countdown |
| 10 | 5-minute lockout with countdown |

**Features:**
- Persistent tracking (survives app restart)
- Exponential backoff for delays
- Visual countdown during lockout
- Number pad disabled during throttling
- Attempts reset on successful authentication

**Storage:**
- Uses EncryptedSharedPreferences
- Tracks: attempt count, last attempt time, lockout end time
- Cleared on successful auth

### Security Modes

| Mode | Behavior |
|------|----------|
| **Security Enabled** | Authentication required at launch; uses biometric → PIN fallback |
| **Security Disabled** | Direct navigation to MainActivity |
| **First Launch** | Prompt to enable security (optional) |

### Settings Toggle Security

**Critical Feature:** Changing security settings (enable OR disable) requires authentication.

**Why?**
- **Disabling without auth** → Unauthorized access
- **Enabling without auth** → Lockout attack (attacker sets their own PIN)

**Implementation:**
```kotlin
// In SettingsFragment.kt
if (currentlyEnabled != isChecked) {
    // ANY change requires authentication
    requestAuthenticationForToggle()
}
```

**Toggle Flow:**
1. User attempts to change security toggle
2. App checks biometric availability
3. Shows biometric prompt OR PIN screen
4. On success: Toggle applied
5. On failure/cancel: Toggle reverted to previous state

**Fallback Chain:**
1. Primary: BiometricPrompt (fingerprint/face/device credential)
2. Fallback: PIN authentication (if set up)
3. Last resort: Prevent toggle + warning message

### Integration in SplashScreen

**Lifecycle:**
1. Show splash animation (3 seconds)
2. Check if user is logged in (DataStore token)
3. If logged in → Check if security enabled
4. If enabled → Request authentication
5. On success → Navigate to MainActivity
6. On failure → Close app

**Error Handling:**
- Biometric lockout → Falls back to PIN
- No auth method → Prompt to set up PIN
- User cancels → App closes (security first)

### Error Scenarios & Recovery

| Scenario | Handling |
|----------|----------|
| No biometric enrolled | Prompt for PIN setup or skip |
| Biometric lockout | Automatic fallback to PIN |
| Too many PIN attempts | Temporary lockout (30s or 5min) |
| No auth method available | Prompt to set up PIN or disable security |
| Authentication cancelled | Close app (prevent bypass) |

### Security Best Practices

✅ **Defense in Depth**: Multiple auth methods (biometric, PIN, device credential)  
✅ **Encrypted Storage**: PIN stored in EncryptedSharedPreferences  
✅ **Anti-Brute-Force**: Progressive throttling and lockouts  
✅ **Fail Secure**: Authentication failure closes app  
✅ **Lockout Prevention**: Auth required to enable security  
✅ **Bypass Prevention**: Auth required to disable security  
✅ **Persistent Security**: Attempt tracking survives restarts  

### User Experience Considerations

**Ease of Use:**
- Fast biometric authentication (< 1 second)
- Clear visual feedback (PIN dots, countdown timers)
- Familiar system UI (BiometricPrompt uses OS design)

**Flexibility:**
- User can enable/disable in Settings
- Automatic fallback to PIN if biometric fails
- Device credential as additional option

**Transparency:**
- Clear error messages
- Explanations for authentication requirements
- Visual indicators for security state

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
- **Biometric Security toggle (SwitchMaterial) - AUTHENTICATION REQUIRED FOR ANY CHANGE**
- Logout button (confirmation dialog)

### Security Toggle Behavior
**⚠️ CRITICAL SECURITY FEATURE**: ANY change to security settings requires authentication

| Action | Authentication Required | Fallback |
|--------|------------------------|----------|
| Enable Security | ✅ YES | Biometric → PIN → Cancel |
| Disable Security | ✅ YES | Biometric → PIN → Cancel |

**Why Both?** A malicious actor could enable security with their own biometric/PIN, locking out the legitimate user. Authentication prevents unauthorized access to security settings in BOTH directions.

#### Toggle Flow (ANY Security Change)
1. User attempts to change security toggle (ON → OFF or OFF → ON)
2. App checks if biometric authentication is available:
   - **Available**: Shows biometric prompt (fingerprint/face/device credential)
   - **Unavailable**: Falls back to PIN authentication
   - **No auth method**: Toggle reverts, shows warning message
3. Authentication options:
   - **Success**: Security state changed, confirmation toast shown
   - **Failure/Cancel**: Toggle reverts to previous state
   - **Error/Lockout**: Falls back to PIN if available
4. Pending state tracked to prevent race conditions

#### Implementation Details
```kotlin
// Authentication required for ANY toggle change
if (currentlyEnabled != isChecked) {
    // State change detected - require authentication
    requestAuthenticationForToggle()
}
```

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
| Network Failure | No Internet Dialog with Retry/Close buttons |
| Loading States | ProgressBar / alpha dimming / swipe spinner |
| Empty Lists | Placeholder item view |
| Validation | Early return with toast or field error |
| Authentication Failure | Toast + app exit |

## 14. Offline Mode Handling

### Files
- `utils/NetworkUtils.kt` - Network connectivity checker
- `utils/NoInternetDialog.kt` - Reusable offline dialog

### Features
- **Proactive Network Check**: Validates connectivity before network requests (Login, Signup, Add Transaction, Logout)
- **Reactive Error Handling**: Detects network errors and shows appropriate dialog (Home, History, Converter)
- **Clear User Communication**: "No Internet Connection" dialog with helpful message
- **Retry Mechanism**: One-tap retry button that re-attempts the original action
- **Non-Blocking**: Close button allows users to dismiss dialog

### Dialog UI
```
┌──────────────────────────────────┐
│  ⚠️ No Internet Connection       │
│                                  │
│  Please check your internet      │
│  connection and try again.       │
│                                  │
│   [Retry]         [Close]        │
└──────────────────────────────────┘
```

### Applied Across All Screens
- ✅ LoginFragment - Checks before login
- ✅ SignupFragment - Checks before signup
- ✅ HomeFragment - Shows dialog on network errors
- ✅ HistoryFragment - Shows dialog on network errors
- ✅ AddTransActivity - Checks before saving transaction
- ✅ ConverterFragment - Shows dialog on rate fetch errors
- ✅ SettingsFragment - Checks before logout

### User Flow Example
1. User attempts action (e.g., Login)
2. App checks: `NetworkUtils.isNetworkAvailable()`
3. If no internet → Show NoInternetDialog
4. User taps **Retry** → Action re-attempted
5. User taps **Close** → Dialog dismissed

## 15. Security Model
| Vector | Mitigation |
|--------|------------|
| Unauthorized Access | Biometric / Device Credential / PIN gate |
| Plain-text credential storage | DataStore + encryption for PIN |
| Stale sessions | Logout clears DataStore |
| Brute force on PIN | ✅ Progressive throttling + temporary lockout (30s/5min) |
| Rapid PIN attempts | ✅ Attempt tracking with escalating delays |
| Unauthorized security changes | ✅ Authentication required for ANY toggle change (enable OR disable) |
| Malicious security lockout | ✅ Prevents attacker from enabling security with their credentials |
| Sensitive token exposure | Not logged in once DataStore cleared |

### Security Toggle Protection
**Critical Security Layer**: Prevents unauthorized users from changing app security settings

**Threat 1 - Disabling Security:** Someone with temporary device access could disable security  
**Threat 2 - Enabling Security:** A malicious actor could enable security with their own biometric/PIN, locking out the legitimate user

**Mitigation:** Requires authentication for ANY toggle change (enable OR disable)
- Biometric authentication via BiometricPrompt (primary)
- PIN authentication via PinLockActivity (fallback)
- Toggle reverts on authentication failure
- Pending state prevents race conditions

**User Experience:** Both enabling and disabling require authentication (maximum security)

## 16. Performance Considerations
- Minimal recomputation: Observers only update UI when LiveData changes
- Chart only re-rendered on summary updates
- History list only rebuilt when transactions change or deletion occurs
- Converter preloads rate once and reuses

## 17. Extensibility Points
| Future Feature | Suggested Approach |
|---------------|--------------------|
| Budget Planning | New module + repository + chart view |
| Recurring Transactions | Add recurrence field to TransactionRequest |
| Export Data (CSV/PDF) | Use WorkManager + file provider |
| Dark Mode | Add theme toggling using DataStore preference |
| Notifications | Use AlarmManager / WorkManager for reminders |
| Multi-Currency | Extend Converter to support dynamic currency list |
| Analytics Trends | Add LineChart / PieChart (MPAndroidChart) |

## 18. App Navigation Structure
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

## 19. Dependencies Summary
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

## 20. Testing Matrix
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
| Toggle biometric ON (enable) - success | Biometric prompt → Success → Security enabled |
| Toggle biometric ON (enable) - cancelled | Biometric prompt → Cancel → Toggle reverts to OFF |
| Toggle biometric ON - no biometric | Falls back to PIN authentication |
| Toggle biometric ON - PIN success | PIN screen → Correct PIN → Security enabled |
| Toggle biometric ON - PIN failed | PIN screen → Wrong PIN → Toggle reverts to OFF |
| Toggle biometric OFF (disable) - success | Biometric prompt → Success → Security disabled |
| Toggle biometric OFF (disable) - cancelled | Biometric prompt → Cancel → Toggle reverts to ON |
| Toggle biometric OFF - no biometric | Falls back to PIN authentication |
| Toggle biometric OFF - PIN success | PIN screen → Correct PIN → Security disabled |
| Toggle biometric OFF - PIN failed | PIN screen → Wrong PIN → Toggle reverts to ON |
| Toggle security (any) - no auth method | Toggle reverts, warning message shown |
| PIN fallback setup | Double entry required, saved encrypted |
| PIN incorrect 3 times | 2-second delay, then 4-second delay |
| PIN incorrect 5 times | 30-second lockout with countdown |
| PIN incorrect 10 times | 5-minute lockout, persists through restart |
| PIN correct after failures | Counter resets, no delay on next attempt |
| No internet on login | "No Internet Connection" dialog shows |
| Tap Retry with internet restored | Login proceeds successfully |
| No internet on add transaction | Dialog shows before save attempt |
| Network error on Home refresh | Dialog shows with retry option |
| Logout | Token cleared + back to Splash |
| Language change | UI restarts with new locale |

## 21. Known Limitations
- No pagination for large transaction sets
- Exchange rate source unspecified (assumes repository provides data)
- No offline-first caching (data requires network, but clear dialogs inform users)
- Locale switching uses deprecated configuration updates (acceptable for MVP; could migrate to AppCompatDelegate APIs)

## 22. Suggested Improvements (Roadmap)
1. Migrate locale handling to modern APIs
2. Implement offline-first caching with local database (Room)
3. Implement dark mode
4. Add biometric lock timeout (auto-unlock window)
5. Add transaction editing feature
6. Add tests (JUnit + Espresso + Robolectric) for critical flows
7. Secure token using encrypted storage (if higher sensitivity required)
8. Introduce pagination & filtering for History
9. Persist chart historical data for analytics
10. Add accessibility improvements (content descriptions, TalkBack labels)
11. Add network retry with exponential backoff
12. Implement background sync for offline actions

## 23. Code Quality Notes
- ViewBinding used for safe view access
- LiveData and Flow separation ensures reactive UI
- Repositories encapsulate data operations
- Minimal logic inside Fragments/Activities (delegated to ViewModels)
- Error messages user-friendly, low technical leakage

## 24. Quick Start Commands
```bash
# Build
./gradlew assembleDebug

# Run tests (if added later)
./gradlew test

# Clean
./gradlew clean
```

## 25. Support & Maintenance
For maintenance:
- Update biometric library to stable when available
- Periodically review security-crypto dependency for CVEs
- Monitor network layer for timeout/retry configuration

## 26. Glossary
| Term | Meaning |
|------|---------|
| PIN | Personal Identification Number (app local fallback) |
| BiometricPrompt | Android system UI for secure biometric/device credential auth |
| DataStore | Jetpack library for typed, async key-value storage |
| MVVM | Model-View-ViewModel architectural pattern |
| EncryptedSharedPreferences | AndroidX secure preferences wrapper |
| Repository | Class handling data operations (network/persistence) |

---
This document provides a comprehensive view of the Expense Tracker app capabilities, structure, and future evolution paths.

