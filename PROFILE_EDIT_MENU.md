# Integrated Profile Edit Menu

This document explains the Profile screen's top-right `Edit profile` menu.

## What Changed

- Added a top-right `Edit profile` button on the Profile screen.
  - Layout: `android/app/src/main/res/layout/fragment_profile.xml`
  - View id: `buttonEditProfile`
  - Label: `@string/edit_profile`

- Removed the separate action buttons from the Profile card:
  - `Change avatar`
  - `Change display name`
  - `Change password`

- The Profile card now shows the username and avatar only.
  - The avatar is positioned lower and aligned left inside the profile card.

- Clicking `Edit profile` opens a menu with:
  - `Change avatar`
  - `Change display name`
  - `Change password`

## Main Code Path

The button is bound in `ProfileFragment.onViewCreated(...)`:

```java
view.findViewById(R.id.buttonEditProfile)
        .setOnClickListener(v -> showEditProfileChooser());
```

`showEditProfileChooser()` owns the menu:

```java
private void showEditProfileChooser()
```

Current dispatch:

```java
if (which == 0) {
    showAvatarSourceChooser();
} else if (which == 1) {
    showDisplayNameDialog();
} else if (which == 2) {
    showPasswordDialog();
}
```

## How To Add Another Profile Edit Action

1. Add a string label in:

```text
android/app/src/main/res/values/strings.xml
```

2. Add the label to the `options` array in `showEditProfileChooser()`.

3. Add a new branch in the `.setItems(...)` handler.

4. Put persistence or reusable business logic in a manager class, not directly in the click handler.

## How To Move The Avatar

The avatar view is:

```xml
<com.example.comp2100miniproject.CircleAvatarImageView
    android:id="@+id/imageAvatar" />
```

Location:

```text
android/app/src/main/res/layout/fragment_profile.xml
```

The current placement is controlled by:

```xml
android:layout_gravity="start"
android:layout_marginStart="8dp"
android:layout_marginTop="22dp"
```

Adjust these values to move it within the profile card.

## Verification

1. Open Profile.
2. Confirm `Edit profile` appears in the top-right of the Profile screen.
3. Confirm the avatar is inside the profile card, lower and left-aligned.
4. Tap `Edit profile`.
5. Confirm all three actions appear.
6. Verify avatar, display name, and password editing still work.
