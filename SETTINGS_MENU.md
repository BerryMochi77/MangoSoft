# Integrated Settings Menu

This document explains the post-feed settings menu added to `MainActivity`.

## What Changed

- Added a top-right `...` settings button on the post feed screen.
  - Layout: `android/app/src/main/res/layout/activity_main.xml`
  - View id: `@+id/buttonSettings`
  - Label string: `@string/settings_overflow_icon`
  - Accessibility description: `@string/settings`

- Added settings dialog logic to `MainActivity`.
  - File: `android/app/src/main/java/com/example/comp2100miniproject/MainActivity.java`
  - Method: `showSettingsDialog()`
  - Current setting row: `Theme: <current mode>`

- Moved manual theme switching into this settings dialog.
  - Login and Profile no longer contain standalone theme buttons.
  - The theme implementation remains in `ThemeModeManager`.

## How It Works

`MainActivity` binds the settings button in `onCreate`:

```java
Button buttonSettings = findViewById(R.id.buttonSettings);
buttonSettings.setOnClickListener(v -> showSettingsDialog());
```

`showSettingsDialog()` builds a small list of settings:

```java
String[] settings = {
        getString(
                R.string.theme_mode_button,
                ThemeModeManager.getSavedModeLabel(this)
        )
};
```

When the user taps the first row, the theme chooser opens:

```java
ThemeModeManager.showModeChooser(this);
```

## How To Add Another Setting

Add another label to the `settings` array in `MainActivity.showSettingsDialog()`:

```java
String[] settings = {
        getString(
                R.string.theme_mode_button,
                ThemeModeManager.getSavedModeLabel(this)
        ),
        getString(R.string.some_new_setting)
};
```

Then handle its index in the click listener:

```java
.setItems(settings, (dialog, which) -> {
    if (which == 0) {
        ThemeModeManager.showModeChooser(this);
    } else if (which == 1) {
        openSomeNewSetting();
    }
})
```

Keep setting-specific logic in a manager class when it needs persistence or is used by multiple screens. For example, theme mode uses `ThemeModeManager` instead of storing preferences directly in `MainActivity`.

## How To Modify The Settings Button

Button text and accessibility label are defined in:

```text
android/app/src/main/res/values/strings.xml
```

Current strings:

```xml
<string name="settings">Settings</string>
<string name="settings_overflow_icon">...</string>
```

The button layout is in:

```text
android/app/src/main/res/layout/activity_main.xml
```

If replacing the text button with a drawable icon later, keep the same id `buttonSettings` unless you also update `MainActivity`.

## Verification

1. Log in or register.
2. Confirm the post feed shows `...` in the top-right corner.
3. Tap `...` and confirm a Settings dialog opens.
4. Tap the theme row and confirm Follow system, Light, and Dark are available.
5. Confirm the old Login and Profile theme buttons are not shown.
