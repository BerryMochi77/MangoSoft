# Avatar Customization

This document explains the profile avatar customization feature.

## What Changed

- Added `AvatarManager`
  - Location: `android/app/src/main/java/com/example/comp2100miniproject/AvatarManager.java`
  - Owns the list of built-in default avatars.
  - Renders the saved avatar into an `ImageView`.
  - Saves either a default avatar key or a gallery image URI through `AuthManager`.

- Updated `AuthManager`
  - Location: `android/app/src/main/java/com/example/comp2100miniproject/auth/AuthManager.java`
  - Adds avatar metadata to `users.json`.
  - New methods:
    - `getAvatar(User user)`
    - `updateAvatar(UUID userId, String source, String value)`
  - Existing users without avatar fields fall back to `avatar_default_1`.

- Updated Profile UI
  - Layout: `android/app/src/main/res/layout/activity_profile.xml`
  - Activity: `android/app/src/main/java/com/example/comp2100miniproject/ProfileActivity.java`
  - Adds avatar preview.
  - Adds `Choose default avatar`.
  - Adds `Choose from album`.

- Added default avatar drawables
  - `avatar_default_1.xml`
  - `avatar_default_2.xml`
  - `avatar_default_3.xml`
  - `avatar_default_4.xml`

## Stored Data Format

Avatar metadata is stored per user in `users.json`:

```json
{
  "avatarSource": "default",
  "avatarValue": "avatar_default_1"
}
```

For gallery images:

```json
{
  "avatarSource": "gallery",
  "avatarValue": "content://..."
}
```

`avatarSource` values:

- `default`: `avatarValue` is a built-in avatar key.
- `gallery`: `avatarValue` is a persisted image URI from Android's document picker.

## How To Display A User Avatar Elsewhere

Use `AvatarManager.displayAvatar(...)`:

```java
AuthManager authManager = new AuthManager(context);
AvatarManager avatarManager = new AvatarManager(authManager);
avatarManager.displayAvatar(user, imageView);
```

The target view must be an `ImageView`.

## How To Add Another Default Avatar

1. Add a drawable XML file under:

```text
android/app/src/main/res/drawable/
```

Example:

```text
avatar_default_5.xml
```

2. Add a label in:

```text
android/app/src/main/res/values/strings.xml
```

Example:

```xml
<string name="avatar_default_5">Orange avatar</string>
```

3. Add it to `DEFAULT_AVATARS` in `AvatarManager`:

```java
new AvatarOption("avatar_default_5", R.string.avatar_default_5, R.drawable.avatar_default_5)
```

The key string should stay stable. Existing user records refer to it.

## How Gallery Selection Works

`ProfileActivity` uses:

```java
ActivityResultContracts.OpenDocument
```

The selected image URI is stored as a string and read back later. The Activity calls:

```java
takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
```

This is required so the app can continue reading the image after the picker closes or the app restarts.

## Verification

At minimum, verify:

1. Open Profile.
2. Select each default avatar and confirm the preview changes.
3. Select an image from the album and confirm the preview changes.
4. Leave Profile and return; the selected avatar should still display.
5. Restart the app; the selected avatar should still display.
6. Existing users with no avatar fields should show the first default avatar.
