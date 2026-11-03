# Torch Tester

A small Android diagnostic app that checks whether the device exposes adjustable flashlight strength through the Android Camera2 API.

## Features

- Detects a camera flash and reads the maximum torch strength level.
- Vertical slider: up increases brightness, down decreases it.
- Torch on/off control.
- Displays whether Android exposes adjustable or fixed torch brightness.
- Automatically turns the torch off when the app leaves the foreground.

## Build with GitHub Actions

Every push to `main` and every manual workflow run builds a debug APK.

1. Open the **Actions** tab.
2. Select **Android Debug APK**.
3. Open the latest successful run.
4. Download the `TorchTester-debug-apk` artifact.
5. Extract the ZIP to get `app-debug.apk`.

## Device compatibility

Adjustable torch strength is exposed by Android 13 (API 33) and newer, but actual support depends on the phone's camera HAL and manufacturer implementation. A device may have a flash while exposing only one brightness level. The app reports the capability exposed by Android; it does not assume that every phone supports dimming.

Minimum Android version: Android 6.0 (API 23). Adjustable strength requires Android 13+ and compatible hardware.
