# 🔧 SETUP INSTRUCTIONS - Monthly Expense Goal Feature

## ⚠️ IMPORTANT: Required Setup Steps

### 1. Add Dependencies to `build.gradle.kts` (Module: app)

```kotlin
dependencies {
    // Existing dependencies...
    
    // ===== ADD THESE FOR MONTHLY GOAL FEATURE =====
    
    // DataStore for local storage
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Hilt support for WorkManager
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")
    
    // Make sure you have these (should already exist):
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
}
```

### 2. Add Permissions to `AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Add these permissions -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    
    <application ...>
        <!-- Your existing app config -->
    </application>
</manifest>
```

### 3. Enable ViewBinding (if not already enabled)

In `build.gradle.kts` (Module: app):

```kotlin
android {
    // ... other config ...
    
    buildFeatures {
        viewBinding = true
        // dataBinding = true (if you use it)
    }
}
```

### 4. Update TransactionResponse Model

The `GoalRepository.kt` expects `date` and `type` fields in `TransactionResponse`. Update your model:

```kotlin
// In TransactionResponse.kt
data class TransactionResponse(
    val id: String,
    val amount: Double,
    val title: String,
    val category: String,
    val date: String,  // ADD THIS (format: "YYYY-MM-DD")
    val type: String,  // ADD THIS ("income" or "expense")
    val notes: String? = null,
    val userId: String,
    val createdAt: String
)
```

**OR** modify `GoalRepository.kt` line 158-165 to match your existing field names:

```kotlin
// Example: If your model uses 'transactionDate' instead of 'date'
val dateParts = transaction.transactionDate.split("-")

// Example: If your model uses 'category' to determine type
transaction.category.equals("expense", ignoreCase = true) &&
```

### 5. Sync Gradle Files

```bash
# In Android Studio:
File → Sync Project with Gradle Files

# OR click "Sync Now" banner at the top
```

### 6. Clean and Rebuild

```bash
# In Android Studio:
Build → Clean Project
Build → Rebuild Project

# OR use Gradle commands:
./gradlew clean
./gradlew build
```

### 7. Initialize Notification Channel

In your `Application` class (or `MainActivity.onCreate`):

```kotlin
// In Application class
@HiltAndroidApp
class ExpenseTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification channel
        GoalNotificationBuilder.createNotificationChannel(this)
    }
}
```

### 8. Add Navigation (Optional)

If using Navigation Component, add to `nav_graph.xml`:

```xml
<fragment
    android:id="@+id/goalFragment"
    android:name="com.example.expensetracker.AppScreens.Goals.GoalFragment"
    android:label="@string/title_monthly_goal"
    tools:layout="@layout/fragment_goal" />
```

If using Bottom Navigation, add menu item:

```xml
<!-- In bottom_navbar.xml -->
<item
    android:id="@+id/nav_goal"
    android:icon="@drawable/outline_add_24"
    android:title="@string/title_monthly_goal" />
```

---

## 🏃 Quick Start After Setup

### 1. Launch Goal Screen

```kotlin
// From any Fragment:
findNavController().navigate(R.id.goalFragment)

// Or directly:
val fragment = GoalFragment()
supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .commit()
```

### 2. Test Notification Permission

On Android 13+ (API 33+), the app will automatically request notification permission when you open the Goal screen for the first time.

### 3. Test WorkManager

To manually trigger the worker for testing:

```kotlin
// In your test/debug code:
val request = OneTimeWorkRequestBuilder<GoalCheckWorker>().build()
WorkManager.getInstance(context).enqueue(request)
```

Check worker status:

```bash
# Via ADB:
adb shell dumpsys jobscheduler
```

---

## 🐛 Troubleshooting

### Error: "Unresolved reference: work"

**Solution:** Add WorkManager dependency and sync Gradle.

### Error: "Unresolved reference: FragmentGoalBinding"

**Solution:** 
1. Enable ViewBinding in build.gradle
2. Sync Gradle
3. Rebuild project

### Error: "Unresolved reference: date"

**Solution:** Update `TransactionResponse` model or modify `GoalRepository` to use your existing field names.

### Error: "Cannot resolve R.layout.dialog_set_goal"

**Solution:** Clean and rebuild project to generate R file.

### WorkManager Not Running

**Solution:**
1. Check battery optimization settings
2. Disable Doze mode for testing:
   ```bash
   adb shell dumpsys deviceidle whitelist +com.example.expensetracker
   ```

### Notifications Not Showing

**Solution:**
1. Check notification permission granted
2. Verify notification channel created
3. Check device notification settings
4. Test with manual worker trigger

---

## ✅ Verification Checklist

Before running the app:

- [ ] Dependencies added to build.gradle
- [ ] Gradle files synced
- [ ] Permissions added to manifest
- [ ] ViewBinding enabled
- [ ] TransactionResponse model updated
- [ ] Project cleaned and rebuilt
- [ ] No compilation errors
- [ ] Notification channel initialized

---

## 🎯 Testing Steps

### 1. Basic Goal Setup:
1. Open app
2. Navigate to Goal screen
3. See "No Goal Set" empty state
4. Tap "Set Monthly Goal"
5. Enter amount: 1000
6. Tap Save
7. ✅ Goal card appears

### 2. Progress Tracking:
1. Goal is set
2. Add some expense transactions via API
3. Tap refresh button
4. ✅ Progress bar updates
5. ✅ Current expenses displayed

### 3. Notifications:
1. Grant notification permission
2. Manually trigger worker (see above)
3. ✅ Notification appears if milestone reached

### 4. Month Reset:
1. Set goal
2. Change device date to next month
3. Open Goal screen
4. ✅ Progress resets to 0%

---

## 📞 Support

If you encounter issues:

1. Check Logcat for error messages
2. Search for tags: "GoalRepository", "GoalCheckWorker", "GoalViewModel"
3. Verify all dependencies installed
4. Ensure device permissions granted
5. Test with different Android versions

---

## 🚀 Ready to Run

Once all setup steps are complete:
1. Run the app
2. Navigate to Goal screen
3. Set your monthly goal
4. Watch automatic progress tracking
5. Receive milestone notifications!

**Happy Goal Tracking!** 🎉

