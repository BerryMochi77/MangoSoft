# Android Social App

This repository now uses the Android project as the main application.

## Structure

```text
android/
  app/             Android UI module
  social-core/     Pure Java social domain module
```

`android/app` contains screens, adapters, layouts, and Android-only code.

`android/social-core` contains reusable social logic:

- posts and messages
- users and user state
- moderation reports
- hidden message filtering
- persistence and data structures

## Build

```powershell
cd android
.\gradlew.bat build
```

If Android Studio has not generated `android/local.properties`, create it locally:

```properties
sdk.dir=C\:\\Users\\52734\\AppData\\Local\\Android\\Sdk
```

`local.properties` is ignored by git.

## Current User Flow

The Android app currently supports:

- viewing the post list
- opening a post
- viewing visible messages
- reporting a message from the message row

The report button calls:

```java
ModerationTools.addReport(messageId, userId, timestamp)
```

Future social features should go into `android/social-core` first, then be surfaced through `android/app`.
