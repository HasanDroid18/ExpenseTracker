# Expense Tracker Application - Complete Documentation
**Last Updated:** November 29, 2025  
**Version:** 2.0  
**Minimum Android Version:** API 24 (Android 7.0)  
**Target Android Version:** API 36  
---
## Table of Contents
1. [App Overview](#app-overview)
2. [Complete Feature List](#complete-feature-list)
3. [Technology Stack](#technology-stack)
4. [Biometric Authentication System](#biometric-authentication-system)
5. [Monthly Goal Notification System](#monthly-goal-notification-system)
---
## App Overview
**Expense Tracker** is a comprehensive financial management Android application built with Kotlin, featuring biometric security and smart budget notifications.
### Key Highlights:
- Secure Authentication with JWT tokens
- Biometric/PIN app lock
- Transaction tracking with categories
- Visual analytics with charts
- Monthly budget goals with notifications
- Currency converter
- Bilingual support (English/Arabic with RTL)
- Offline-ready with error handling
---
## Complete Feature List
### Authentication & Security
- User Registration & Login
- JWT token-based authentication
- Biometric lock (fingerprint/face)
- PIN fallback with encryption
- Anti-brute-force protection
- Secure encrypted storage
### Financial Management
- Add/Delete transactions
- Income & Expense categorization
- Transaction history
- Amount formatting
- Timestamp-based records
### Analytics
- Dashboard with balance summary
- Bar chart visualization
- Real-time updates
- Pull-to-refresh
### Goal Management
- Set/Edit/Delete monthly goals
- Visual progress tracking
- Auto month reset
- Smart notifications (20%, 50%, 80%, 100%)
- Background sync via WorkManager
### Additional Features
- Currency converter (USD ↔ LBP)
- Language selection (English/Arabic)
- Settings management
- Offline error handling
- Network detection
---
## Technology Stack
### Core Technologies
- **Kotlin** - Primary language
- **MVVM Architecture** - Design pattern
- **Coroutines** - Async programming
- **Hilt** - Dependency injection
- **Retrofit** - REST API client
- **DataStore** - Preferences storage
- **WorkManager** - Background jobs
### Security
- **AndroidX Biometric** - Biometric auth API
- **Security Crypto** - Encrypted storage
- **EncryptedSharedPreferences** - Secure PIN storage
### UI & Visualization
- **Material Components** - Material Design 3
- **MPAndroidChart** - Charts library
- **ViewBinding** - Type-safe views
- **Lottie** - Animations
### Notifications
- **NotificationCompat** - Backward compatibility
- **NotificationChannel** - Android 8.0+ channels
- **PendingIntent** - Notification actions
---
## Biometric Authentication System
### Overview
Comprehensive security layer with fingerprint/face recognition and PIN fallback.
### Components
- **BiometricAuthManager** - Biometric authentication handler
- **PinManager** - Encrypted PIN storage
- **PinLockActivity** - PIN UI with number pad
- **PinAttemptManager** - Anti-brute-force protection
### Authentication Flow
App Launch → Check security enabled → Biometric prompt → Success/Fallback to PIN → Enter app
### Anti-Brute-Force Protection
| Attempts | Action |
|----------|--------|
| 1-2 | Immediate retry |
| 3 | 2-second delay |
| 4 | 4-second delay |
| 5 | 30-second lockout |
| 10 | 5-minute lockout |
### Security Features
- Defense in depth (multiple auth methods)
- Encrypted PIN storage (AES256_GCM)
- Progressive throttling
- Persistent tracking
- Authentication required for security toggle changes
---
## Monthly Goal Notification System
### Overview
Local-first goal tracking with smart milestone notifications and auto-reset.
### Key Features
- Set/Edit/Delete monthly goals
- Auto progress tracking from API
- Color-coded progress bars
- Smart notifications at 20%, 50%, 80%, 100%
- Auto month reset
- WorkManager background sync (every 6 hours)
### Data Storage
All data stored locally in DataStore:
- Goal amount
- Current month/year
- Notification flags per milestone
- Goal creation timestamp
### Notification Milestones
| Milestone | Title | Priority |
|-----------|-------|----------|
| 20% | "20% of Monthly Goal Reached! 📊" | Default |
| 50% | "50% of Monthly Goal Reached! ⚠️" | Default |
| 80% | "80% of Monthly Goal Reached! ⚠️" | High |
| 100% | "Monthly Goal Reached! 🚨" | Max |
### Features
- Once per month per milestone
- Works Android 7 to latest
- Immediate notifications while using app
- Background checks every 6 hours
- Permission handling for Android 13+
- Custom sounds and vibration patterns
### Implementation Details
- Uses created_in timestamp for expense filtering
- Only counts expenses after goal creation
- Progress resets automatically each month
- Notification flags clear on month change
- Goal amount persists across months
---
**End of Documentation** - For detailed implementation, see individual component documentation files.
