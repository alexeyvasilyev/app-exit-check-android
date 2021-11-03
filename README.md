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
  implementation 'com.github.alexeyvasilyev:app-exit-check-android:0.0.3'
}
```

## How to use:
```java
// Put in Application onCreate()
if (BuildConfig.DEBUG) {
    appExitCheck = new AppExitCheck.Builder(this)
        .withListener(new AppExitCheck.AppExitCheckListener() {
            @Override
            public void onFailedLeakedTrafficDetected(long leakedBytes) {
                showLeaksNotification(
                        "App leaked traffic detected",
                        "Consumed " + leakedBytes + " bytes within 2 sec in 30 sec after app closed."
                );
            }

            @Override
            public void onSuccess() {
            }
        })
        .withThreshold(100)
        .withTimeout(30000)
        .withInterval(2000)
        .build();
}
    
// When app goes to background put
if (BuildConfig.DEBUG && appExitCheck != null) {
    appExitCheck.scheduleAppExitCheck();
}

// When app goes to foreground
if (BuildConfig.DEBUG && appExitCheck != null) {
    appExitCheck.cancelAppExitCheck();
}

```
