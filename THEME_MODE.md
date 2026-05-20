# Theme Mode Switching

This document explains the manual light/dark/system theme switching feature.

## What Changed

- Added `ThemeModeManager`
  - Location: `android/app/src/main/java/com/example/comp2100miniproject/ThemeModeManager.java`
  - Owns the theme mode enum: `SYSTEM`, `LIGHT`, `DARK`
  - Saves the selected mode in `SharedPreferences`
  - Applies the selected mode through `AppCompatDelegate.setDefaultNightMode(...)`
  - Exposes `showModeChooser(AppCompatActivity activity)` for UI entry points

- Added `SocialModerationApplication`
  - Location: `android/app/src/main/java/com/example/comp2100miniproject/SocialModerationApplication.java`
  - Applies the saved theme mode when the app process starts

- Updated `AndroidManifest.xml`
  - Registers `SocialModerationApplication` with:

```xml
android:name=".SocialModerationApplication"
```

- Added UI entry point
  - Main post feed screen: `activity_main.xml` + `MainActivity`
  - The user opens the integrated settings button (`...`) and selects the theme mode row.

- Added dark color resources
  - Location: `android/app/src/main/res/values-night/colors.xml`
  - Existing color names are reused, so layouts using `@color/text_primary`, `@color/surface`, etc. switch automatically.

## How To Use It From Another Screen

Add a settings row, button, or menu item, then call:

```java
ThemeModeManager.showModeChooser(this);
```

The Activity must extend `AppCompatActivity`.

Example:

```java
new AlertDialog.Builder(this)
        .setTitle(R.string.settings)
        .setItems(new String[] {
                getString(
                        R.string.theme_mode_button,
                        ThemeModeManager.getSavedModeLabel(this)
                )
        }, (dialog, which) -> ThemeModeManager.showModeChooser(this))
        .show();
```

When the user selects a mode, the manager saves it and applies it globally. AppCompat recreates affected activities as needed.

## How To Modify The Theme Choices

Theme choices are defined in:

```java
ThemeModeManager.Mode
```

Current modes:

- `SYSTEM`: follows the device setting
- `LIGHT`: forces light mode
- `DARK`: forces dark mode

If you add another mode, also add a string label in:

```text
android/app/src/main/res/values/strings.xml
```

## How To Modify Light And Dark Colors

Light mode colors:

```text
android/app/src/main/res/values/colors.xml
```

Dark mode colors:

```text
android/app/src/main/res/values-night/colors.xml
```

Use the same color resource names in both files. Existing layouts already reference these shared names:

- `@color/surface`
- `@color/card_surface`
- `@color/border`
- `@color/text_primary`
- `@color/text_secondary`
- `@color/accent`
- `@color/warning`
- `@color/warning_soft`

Do not hard-code new UI colors in layout XML. Add or update named colors in both `values/colors.xml` and `values-night/colors.xml` instead.

## Verification

At minimum, verify:

1. Log in or register and open the post feed.
2. Tap the top-right `...` settings button.
3. Select `Theme: ...`, then choose Follow system, Light, or Dark.
4. The selected mode persists after closing and reopening the app.
5. Existing card, text, border, and warning colors remain readable in both modes.
