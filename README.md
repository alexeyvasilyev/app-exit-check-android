# app-exit-check-android
Android library designed for finding leaked network connections on app exit.

[![Release](https://jitpack.io/v/alexeyvasilyev/app-exit-check-android.svg)](https://jitpack.io/#alexeyvasilyev/app-exit-check-android)

## Compile

To use this library in your project with gradle add this to your build.gradle:

```gradle
allprojects {
  repositories {
    maven { url 'https://jitpack.io' }
  }
}
dependencies {
  implementation 'com.github.alexeyvasilyev:app-exit-check-android:0.0.4'
}
```

## How to use:
```kotlin
private var appExitCheck: AppExitCheck? = null

// Put in Application onCreate()
if (BuildConfig.DEBUG) {
    AppExitCheck.Builder(this)
        // Log debug data on logcat
        .withDebug(true)
        // Show warning only if at least 100 bytes leaked
        .withThreshold(100)
        // Start checking after 2 sec app closed
        .withTimeout(2000)
        // Measure traffic consumption within 5 sec
        .withInterval(5000)
        .withListener(object : AppExitCheckListener {
            override fun onFailedLeakedTrafficDetected(leakedBytes: Long) {
                showLeaksNotification(
                    "App leaked traffic detected",
                    "Consumed $leakedBytes bytes within 5 sec after app closed."
                )
            }

            override fun onSuccess() {
                Log.i(TAG, "No leaks detected")
            }
        })
        .build().also { appExitCheck = it }
}

// When app goes to background put
if (BuildConfig.DEBUG) {
    appExitCheck?.scheduleAppExitCheck()
}

// When app goes to foreground
if (BuildConfig.DEBUG) {
    appExitCheck?.cancelAppExitCheck()
}

```
