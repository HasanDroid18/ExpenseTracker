# Change Password Feature - Quick Start Guide

## How to Use

### For Users
1. Open the app and navigate to **Settings** screen
2. Scroll to find **"Change Password"** row (has a lock icon 🔒)
3. Tap on **"Change Password"**
4. A dialog will appear with three fields:
   - **Old Password**: Enter your current password
   - **New Password**: Enter your desired new password
   - **Confirm New Password**: Re-enter the new password
5. Tap **"Change Password"** button
6. Wait for the loading indicator
7. Success message will appear and dialog will close
8. Your password is now updated!

### Validation Rules (User-Friendly)
- All fields are required - don't leave any empty!
- New password and confirm password must match
- New password must be different from your old password
- Must have internet connection

### Error Messages You Might See

**Client-Side Errors (Instant):**
- "Old password is required" - You forgot to enter old password
- "New password is required" - You forgot to enter new password
- "Please confirm your new password" - You forgot to confirm
- "Passwords do not match" - New password ≠ Confirm password
- "New password must be different from old password" - Can't reuse same password
- "Network error. Please check your connection." - No internet

**Server-Side Errors (After submitting):**
- "Old password is incorrect" - Wrong current password entered
- "User not found" - Account issue, try logging in again
- "oldPassword and newPassword are required" - Server validation issue
- "New password must be different from old password" - Server detected duplicate
- "Internal server error" - Server problem, try again later
- "Authentication required. Please login again." - Session expired

### Tips
- Use the 👁️ icon to show/hide passwords while typing
- Tap **Cancel** to close dialog without saving
- Make sure you remember your new password!
- The dialog won't close until password is successfully changed or you tap Cancel

## For Developers

### Testing the Feature

#### Manual Testing
```kotlin
// Test credentials (example)
Old Password: "hasan@2026"
New Password: "hasan@2025"
Confirm: "hasan@2025"
```

#### Test Cases

1. **Happy Path**
   - Enter correct old password
   - Enter new valid password
   - Confirm matches
   - ✅ Should succeed with success message

2. **Validation Errors**
   - Leave old password empty → "Old password is required"
   - Leave new password empty → "New password is required"
   - Leave confirm empty → "Please confirm your new password"
   - New ≠ Confirm → "Passwords do not match"
   - Old = New → "New password must be different from old password"

3. **Backend Errors**
   - Wrong old password → "Old password is incorrect" (401)
   - Invalid user → "User not found" (404)
   - Server error → "Internal server error" (500)

4. **Network Issues**
   - Disable WiFi/data → "Network error. Please check your connection."
   - Enable retry dialog appears

5. **Authentication**
   - Clear token from DataStore → "Authentication required. Please login again."

### Code Integration

The feature is fully integrated:
```kotlin
// In SettingsFragment.kt
binding.rowChangePassword.setOnClickListener {
    showChangePasswordDialog()
}

// In SettingsViewModel.kt
viewModel.changePassword(oldPassword, newPassword)

// In SettingsRepository.kt
suspend fun changePassword(oldPassword: String, newPassword: String): Result<String>
```

### Observing State
```kotlin
viewModel.changePasswordState.observe(viewLifecycleOwner) { result ->
    result.onSuccess { message ->
        // Show success toast
        // Clear fields
        // Dismiss dialog
    }
    result.onFailure { error ->
        // Show error toast
    }
}
```

### Backend API
- **Endpoint**: `POST /auth/change-password`
- **Authorization**: `Bearer <token>` (auto-included)
- **Request Body**:
```json
{
  "oldPassword": "string",
  "newPassword": "string"
}
```

### Supported Languages
- ✅ English (en)
- ✅ Arabic (ar) with **full RTL support**
  - Dialog layout direction adjusts automatically
  - Text input fields align properly (right-to-left)
  - Icons position correctly in RTL mode
  - All edit boxes throughout the app support RTL

### Architecture
```
UI (Fragment) → ViewModel → Repository → API Service → Backend
      ↑                                        ↓
      └────── LiveData ← Result ← Response ────┘
```

## Troubleshooting

### Dialog doesn't appear
- Check `R.layout.dialog_change_password` exists
- Check `binding.rowChangePassword` is not null

### "Too many arguments" compile error
- Clean and rebuild project: `./gradlew clean build`
- Invalidate caches in Android Studio

### Token issues
- Verify UserDataStore.tokenFlow.first() returns token
- Check user is logged in
- Verify token is saved during login

### Backend 401 Unauthorized
- ✅ **FIXED**: Token is now included in Authorization header
- Token format: `Bearer <token>`
- Token retrieved from UserDataStore

### Strings not found
- Verify strings.xml has all required strings
- Check strings-ar.xml for Arabic translations
- Clean and rebuild

## Future Enhancements (Optional)

- [ ] Add password strength indicator
- [ ] Add minimum password length validation
- [ ] Add password complexity requirements
- [ ] Show password requirements before user types
- [ ] Add "Forgot Password" option
- [ ] Add password change history
- [ ] Add email notification on password change
- [ ] Add password visibility toggle memory
- [ ] Add biometric authentication before change
- [ ] Add password generator

## Support

If you encounter any issues:
1. Check the error message displayed
2. Verify internet connection
3. Try logging out and back in
4. Check backend API logs
5. Review CHANGE_PASSWORD_IMPLEMENTATION.md for details

