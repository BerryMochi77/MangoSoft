# Avatar Source Selection Button

This document explains the Profile avatar source selection UI.

## What Changed

- Replaced the two separate Profile buttons:
  - `Choose default avatar`
  - `Choose from album`

- Avatar editing is now reached from the integrated Profile edit menu:
  - Top-right button text: `Edit profile`
  - View id: `buttonEditProfile`
  - Layout: `android/app/src/main/res/layout/fragment_profile.xml`
  - First menu row: `Change avatar`

- Clicking `Edit profile` then `Change avatar` opens an `AlertDialog` asking which avatar source to use:
  - `Choose default avatar`
  - `Choose from album`

- Existing avatar behavior is unchanged:
  - Default avatar selection still calls `showDefaultAvatarChooser()`.
  - Album selection still calls `chooseGalleryAvatar()`.
  - Gallery images still go through `AvatarCropActivity` before being saved.

## Main Code Path

The top-level edit button is wired in `ProfileFragment.onViewCreated(...)`:

```java
view.findViewById(R.id.buttonEditProfile)
        .setOnClickListener(v -> showEditProfileChooser());
```

`showEditProfileChooser()` dispatches the avatar row to:

```java
private void showAvatarSourceChooser()
```

Current logic:

```java
if (which == 0) {
    showDefaultAvatarChooser();
} else if (which == 1) {
    chooseGalleryAvatar();
}
```

## How To Add Another Avatar Source

1. Add a new string in:

```text
android/app/src/main/res/values/strings.xml
```

2. Add the label to the `options` array in `ProfileFragment.showAvatarSourceChooser()`.

3. Add another branch in the `.setItems(...)` handler.

Example:

```java
} else if (which == 2) {
    openCameraAvatarPicker();
}
```

Keep source-specific persistence inside `AvatarManager` or another manager class. Avoid writing avatar storage logic directly in the click listener.

## How To Rename The Button

Change:

```xml
<string name="change_avatar">Change avatar</string>
```

in:

```text
android/app/src/main/res/values/strings.xml
```

Do not change the view id `buttonEditProfile` unless you also update `ProfileFragment`.

## Verification

1. Open Profile.
2. Tap `Edit profile`.
3. Tap `Change avatar` and confirm the source chooser appears.
4. Choose `Choose default avatar` and verify the default avatar picker still works.
5. Choose `Choose from album` and verify the album picker and crop flow still work.
