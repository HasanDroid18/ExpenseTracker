# Change Password Feature Implementation

## Overview
Complete implementation of Change Password feature using MVVM architecture pattern with authentication token support.

## Files Created

### 1. Data Classes
- **ChangePasswordRequest.kt** - Request data class with oldPassword and newPassword fields
- **ChangePasswordResponse.kt** - Response data class with message field

### 2. UI Layout
- **dialog_change_password.xml** - Material Design dialog with:
  - Old password input field
  - New password input field
  - Confirm password input field
  - Loading progress bar
  - Cancel and Confirm buttons

## Files Modified

### 1. API Service (`ApiService.kt`)
```kotlin
@POST("auth/change-password")
suspend fun changePassword(
    @Header("Authorization") token: String,
    @Body request: ChangePasswordRequest
): Response<ChangePasswordResponse>
```
- Added `Authorization` header with Bearer token
- Endpoint: `POST auth/change-password`

### 2. Repository (`SettingsRepository.kt`)
Added `changePassword()` function with:
- Token retrieval from UserDataStore using Flow.first()
- Authentication validation
- API call with Bearer token
- Comprehensive error handling for all backend response codes:
  - 200: Success
  - 400: Validation errors (missing fields, same password)
  - 401: Incorrect old password
  - 404: User not found
  - 500: Server error
  - Network errors

### 3. ViewModel (`SettingsViewModel.kt`)
Added:
- `_changePasswordState` LiveData for UI observation
- `_isLoading` LiveData for loading state
- `changePassword()` function that calls repository

### 4. Fragment (`SettingsFragment.kt`)
Added:
- Click listener for Change Password row
- `showChangePasswordDialog()` function with:
  - Client-side validation
  - Empty field checks
  - Password match validation
  - Different from old password check
  - Network connectivity check
  - Loading state management
  - Toast messages for all responses
  - Field clearing on success

### 5. Settings Layout (`fragment_settings.xml`)
Added Change Password row between Biometric Security and About Us:
- Lock icon
- Title: "Change Password"
- Chevron navigation icon
- Clickable row with ripple effect

### 6. String Resources
**English (`strings.xml`):**
- title_change_password
- hint_old_password
- hint_new_password
- hint_confirm_password
- action_change_password
- error_empty_old_password
- error_empty_new_password
- error_empty_confirm_password
- error_passwords_not_match
- error_same_password
- success_password_changed
- error_network

**Arabic (`strings-ar.xml`):**
- All strings translated to Arabic with RTL support

## Validation Rules

### Client-Side (Before API Call)
1. ✅ Old password field must not be empty
2. ✅ New password field must not be empty
3. ✅ Confirm password field must not be empty
4. ✅ New password must match confirm password
5. ✅ New password must be different from old password
6. ✅ Network connectivity check

### Server-Side (Backend Responses)
1. ✅ 400: "oldPassword and newPassword are required"
2. ✅ 400: "New password must be different from old password"
3. ✅ 404: "User not found"
4. ✅ 401: "Old password is incorrect"
5. ✅ 200: "Password changed successfully"
6. ✅ 500: "Internal server error"

## Features

### Security
- ✅ Requires authentication token (Bearer token)
- ✅ Token retrieved from UserDataStore
- ✅ Validates user is logged in before allowing change
- ✅ Password fields use `inputType="textPassword"`
- ✅ Toggle password visibility with eye icon

### UX
- ✅ Material Design dialog
- ✅ Real-time error messages on input fields
- ✅ Loading indicator during API call
- ✅ Buttons disabled while loading
- ✅ Network connectivity check with retry
- ✅ Toast messages for all outcomes
- ✅ Auto-clear fields on success
- ✅ Cancel button to dismiss dialog

### Localization
- ✅ Full English support
- ✅ Full Arabic support with RTL
- ✅ All strings externalized in resources

## User Flow

1. User taps "Change Password" in Settings
2. Dialog appears with three input fields
3. User enters old password, new password, and confirms
4. Clicks "Change Password" button
5. Client validation runs:
   - Empty field checks
   - Password match check
   - Different from old check
6. Network check performed
7. Loading indicator shown, buttons disabled
8. API call sent with Bearer token
9. Backend validates and responds
10. Success: Toast shown, dialog dismissed, fields cleared
11. Error: Toast shown with specific error message

## Error Handling

### Network Errors
- Connection timeout
- No internet connection
- Server unreachable

### Authentication Errors
- No token found (requires re-login)
- Invalid token (401)

### Validation Errors
- Backend-specific validation messages
- Parsed from error response body

### UI Feedback
- All errors shown via Toast messages
- Long duration for readability
- Specific messages for each error type

## Testing Checklist

- [ ] Empty old password shows error
- [ ] Empty new password shows error
- [ ] Empty confirm password shows error
- [ ] Mismatched passwords show error
- [ ] Same old and new password show error
- [ ] Network error handled gracefully
- [ ] Loading state shows/hides correctly
- [ ] Success clears fields and closes dialog
- [ ] Cancel button works
- [ ] Token authentication works
- [ ] All backend error codes handled
- [ ] English translations correct
- [ ] Arabic translations correct
- [ ] RTL layout works in Arabic

## Dependencies

No new dependencies required. Uses existing:
- Retrofit for networking
- DataStore for token storage
- Material Design components
- LiveData for state management
- Coroutines for async operations

## Code Quality

- ✅ MVVM architecture followed
- ✅ Clean separation of concerns
- ✅ Comprehensive comments
- ✅ Proper error handling
- ✅ Reusable code patterns
- ✅ Follows project conventions
- ✅ No hardcoded strings
- ✅ Null safety handled

## Backend Integration

### Request
```json
{
  "oldPassword": "user_old_password",
  "newPassword": "user_new_password"
}
```

### Headers
```
Authorization: Bearer <token_from_datastore>
Content-Type: application/json
```

### Response (Success)
```json
{
  "message": "Password changed successfully"
}
```

### Response (Error)
```json
{
  "message": "Old password is incorrect"
}
```

## Notes
- Token is automatically retrieved from UserDataStore
- If token is missing, user is prompted to login again
- All backend messages are displayed to user
- Dialog uses transparent background for rounded corners
- Compatible with existing biometric/PIN security features

