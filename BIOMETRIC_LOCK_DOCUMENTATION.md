# App Lock Feature - Biometric & PIN Authentication

## Overview
This document describes the biometric app lock implementation for the Expense Tracker app. The app now requires authentication using fingerprint, face recognition, or device PIN/pattern before allowing access to the main content.

## Features
- ✅ Biometric authentication (fingerprint, face recognition)
- ✅ Device credentials fallback (system PIN, pattern, password)
- ✅ Custom in-app PIN fallback when biometrics unavailable
- ✅ Auto-lock when app goes to background and returns
- ✅ Secure PIN storage using EncryptedSharedPreferences
- ✅ App closes on authentication cancellation

## Architecture

### Components Created

#### 1. **BiometricAuthManager.kt**
Location: `app/src/main/java/com/example/expensetracker/auth/biometric/`

Manages biometric authentication using AndroidX BiometricPrompt:
- Checks device biometric capability
- Configures and displays biometric prompt
- Handles success, error, and failure callbacks
- Supports both biometric and device credentials

```kotlin
// Usage example:
val authManager = BiometricAuthManager(activity)
authManager.authenticate(
    onSuccess = { /* User authenticated */ },
    onError = { errorCode, message -> /* Handle error */ },
    onFailure = { /* Authentication failed, can retry */ }
)
```

#### 2. **AppLockLifecycleObserver.kt**
Location: `app/src/main/java/com/example/expensetracker/auth/biometric/`

Monitors app lifecycle to trigger authentication:
- Detects when app moves from background to foreground
- Triggers authentication callback automatically
- Prevents redundant authentication requests

#### 3. **PinManager.kt**
Location: `app/src/main/java/com/example/expensetracker/auth/biometric/`

Securely manages in-app PIN:
- Stores PIN using EncryptedSharedPreferences
- Verifies PIN attempts
- Allows PIN updates and clearing
- Uses AES256_GCM encryption

#### 4. **PinLockActivity.kt**
Location: `app/src/main/java/com/example/expensetracker/auth/biometric/`

Fallback PIN entry screen:
- Allows users to set up a 4-digit PIN
- Verifies PIN for existing users
- Custom number pad UI
- Returns authentication result to caller

#### 5. **MainActivity.kt** (Updated)
Orchestrates the authentication flow:
- Initializes biometric authentication on startup
- Handles biometric success/failure
- Falls back to PIN when biometrics unavailable
- Closes app on authentication failure

## Authentication Flow

```
App Start
    ↓
Check Biometric Availability
    ↓
    ├─→ Biometric Available
    │       ↓
    │   Show Biometric Prompt
    │       ↓
    │       ├─→ Success → Grant Access
    │       ├─→ Failure → Allow Retry
    │       ├─→ Cancel → Close App
    │       └─→ Too Many Attempts → Fallback to PIN
    │
    └─→ Biometric NOT Available
            ↓
        Show PIN Entry
            ↓
            ├─→ PIN Set → Verify PIN
            │       ↓
            │       ├─→ Correct → Grant Access
            │       └─→ Incorrect → Allow Retry
            │
            └─→ No PIN Set → Setup PIN
                    ↓
                Enter PIN Twice
                    ↓
                    ├─→ Match → Save & Grant Access
                    └─→ No Match → Retry Setup
```

## Background/Foreground Detection

The app automatically re-locks when:
1. User exits the app (presses Home)
2. User switches to another app
3. App is sent to background for any reason

When returning to the app, authentication is required again.

## Device Requirements

### Minimum Requirements
- Android API Level 24 (Android 7.0)
- Device with biometric hardware (fingerprint sensor, face unlock) OR
- Device with screen lock enabled (PIN, pattern, password)

### Recommended
- Android API Level 28+ for best biometric experience
- Enrolled biometrics (fingerprint or face)

## Configuration

### Permissions Required
Already added to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

### Dependencies Added
Already added to `app/build.gradle.kts`:
```kotlin
implementation("androidx.biometric:biometric:1.2.0-alpha05")
implementation("androidx.security:security-crypto:1.1.0-alpha06")
implementation("androidx.lifecycle:lifecycle-process:2.6.1")
```

## Testing Guide

### Testing Biometric Authentication

#### On Real Device:
1. Ensure fingerprint or face is enrolled in device settings
2. Launch the app
3. Biometric prompt should appear
4. Test with correct biometric → App should unlock
5. Test with incorrect biometric → Should allow retry
6. Cancel the prompt → App should close

#### On Emulator:
Android Studio Emulator supports fingerprint simulation:
1. Open Extended Controls (... icon in emulator toolbar)
2. Go to "Fingerprint" section
3. Click "Touch the sensor" after biometric prompt appears

### Testing PIN Fallback

#### Test PIN Setup:
1. Use a device without biometrics enrolled
2. Launch the app
3. Dialog should prompt for PIN fallback
4. Enter a 4-digit PIN twice
5. App should unlock after successful setup

#### Test PIN Verification:
1. Close and reopen the app
2. PIN entry screen should appear
3. Enter correct PIN → App unlocks
4. Enter incorrect PIN → Error shown, can retry

#### Test PIN Security (Throttling & Lockout):
1. **Test Progressive Delay:**
   - Enter wrong PIN 3 times → 2-second delay message appears
   - Enter wrong PIN 4th time → 4-second delay (increases progressively)
   - Number pad disabled during delay with countdown

2. **Test 30-Second Lockout:**
   - Enter wrong PIN 5 times total
   - "Locked out. Try again in X seconds" message appears
   - Number pad visually dimmed and disabled
   - Countdown timer shows remaining seconds
   - After 30s → Number pad re-enables, can try again

3. **Test 5-Minute Lockout:**
   - Enter wrong PIN 10 times total
   - "Locked out. Try again in X minutes" message appears
   - Lockout persists even if app is closed and reopened
   - After 5 minutes → Can attempt again

4. **Test Attempt Reset:**
   - Enter correct PIN after failed attempts
   - Attempt counter resets to 0
   - No delays or lockouts on next authentication session

### Testing Background Lock
1. Authenticate and enter the app
2. Press Home button to background the app
3. Return to the app
4. Authentication should be required again

## Security Considerations

### PIN Storage
- PINs are stored using EncryptedSharedPreferences
- Uses AES256_GCM encryption
- Master key managed by Android Keystore
- Secure against casual app inspection

### PIN Security Features (Anti-Brute-Force)
**Progressive Throttling:**
- After 3 failed attempts: 2-second delay (increases by 2s per attempt)
- After 4 failed attempts: 4-second delay
- Continues increasing with each failure

**Temporary Lockout:**
- After 5 failed attempts: 30-second lockout
- After 10 failed attempts: 5-minute lockout
- Number pad disabled during lockout
- Visual countdown timer shown

**Attempt Tracking:**
- Failed attempts persisted across app restarts
- Warning messages displayed as threshold approaches
- Successful authentication resets attempt counter
- Lockout state survives app backgrounding

### Biometric Security
- Uses BIOMETRIC_STRONG authenticators
- Falls back to device credentials (system PIN/pattern)
- No biometric data is stored by the app
- All biometric handling done by Android system

### Best Practices Implemented
✅ App closes on authentication failure (prevents unauthorized access)
✅ No bypass mechanisms implemented
✅ Re-authentication required on app resume
✅ Secure credential storage with encryption
✅ Graceful error handling
✅ Brute-force protection with progressive delays
✅ Temporary lockout after repeated failures
✅ Persistent attempt tracking

## Troubleshooting

### Issue: "Biometric authentication is not available"
**Solution:** 
- Check if device has biometric hardware
- Ensure at least one fingerprint/face is enrolled
- Verify device has screen lock (PIN/pattern) enabled
- Use PIN fallback option

### Issue: "No biometrics enrolled" dialog
**Solution:**
- Go to device Settings → Security → Fingerprint/Face
- Enroll at least one biometric
- OR use the PIN fallback when prompted

### Issue: App crashes on authentication
**Solution:**
- Check Logcat for error messages
- Ensure all dependencies are synced
- Verify Gradle sync completed successfully
- Clean and rebuild project

### Issue: PIN not working after restart
**Solution:**
- This is normal - EncryptedSharedPreferences may require re-initialization
- Should work after one app restart
- If persistent, clear app data and set up PIN again

## Future Enhancements (Optional)

Potential improvements for future versions:
- [ ] Timeout setting (require auth after X minutes)
- [ ] PIN length customization (4-6 digits)
- [ ] Biometric-only mode (disable PIN fallback)
- [ ] Failed attempt counter with lockout
- [ ] Pattern lock option
- [ ] Settings screen to manage authentication preferences
- [ ] Face authentication specific error handling
- [ ] Iris scanner support (Samsung devices)

## Code Structure

```
app/src/main/java/com/example/expensetracker/
├── MainActivity.kt (Updated with auth logic)
└── auth/
    └── biometric/
        ├── BiometricAuthManager.kt (Biometric prompt handler)
        ├── AppLockLifecycleObserver.kt (Background detection)
        ├── PinManager.kt (PIN storage & verification)
        └── PinLockActivity.kt (PIN entry UI)

app/src/main/res/
├── layout/
│   └── activity_pin_lock.xml (PIN entry screen)
├── drawable/
│   └── pin_dot_selector.xml (PIN dot indicator)
└── values/
    └── themes.xml (PIN button style)
```

## Support

For issues or questions about the app lock feature:
1. Check this documentation
2. Review the inline code comments
3. Check Android Biometric documentation: https://developer.android.com/training/sign-in/biometric-auth

---

**Last Updated:** November 2025  
**Feature Version:** 1.0  
**Min Android Version:** API 24 (Android 7.0)  
**Target Android Version:** API 36

