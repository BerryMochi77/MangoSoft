# Profile Field Editing Dialogs

This document explains the Profile display-name and password editing flow.

## What Changed

- Removed the always-visible Profile text fields:
  - `inputDisplayName`
  - `inputNewPassword`
  - `buttonSaveProfile`

- Added one integrated Profile edit button in `fragment_profile.xml`:
  - `buttonEditProfile`
  - Text: `Edit profile`

- Clicking `Edit profile` opens a menu. Choosing either field opens an `AlertDialog` with one input field.
  - `Change display name` opens a display-name field pre-filled with the current display name.
  - `Change password` opens a password field.

- Avatar editing already uses a button-driven flow. Display name and password now follow the same pattern.

## Main Code Path

The button is wired in `ProfileFragment.onViewCreated(...)`:

```java
view.findViewById(R.id.buttonEditProfile)
        .setOnClickListener(v -> showEditProfileChooser());
```

`showEditProfileChooser()` dispatches menu rows to `showDisplayNameDialog()` or `showPasswordDialog()`.

The dialog methods are:

```java
private void showDisplayNameDialog()
private void showPasswordDialog()
```

Both methods create an `EditText`, show it in an `AlertDialog`, and then call one of:

```java
private void saveDisplayName(String displayName)
private void savePassword(String newPassword)
```

Both save methods still use:

```java
AuthManager.updateProfile(...)
```

## How To Change The Button Labels

Update these strings:

```xml
<string name="change_display_name">Change display name</string>
<string name="change_password">Change password</string>
```

Location:

```text
android/app/src/main/res/values/strings.xml
```

## How To Add Another Editable Profile Field

1. Add a new row label to `ProfileFragment.showEditProfileChooser()`.

2. Add a new branch in its `.setItems(...)` handler.

3. Add a dialog method similar to:

```java
private void showDisplayNameDialog()
```

4. Keep persistence in `AuthManager` or another manager class. Do not store profile data directly in the Fragment.

## Verification

1. Open Profile.
2. Confirm display name and password fields are not always visible.
3. Tap `Edit profile`, then `Change display name`; confirm a single display-name field appears.
4. Save a valid name and confirm the displayed username line updates.
5. Tap `Edit profile`, then `Change password`; confirm a single password field appears.
6. Save a valid password and confirm login works with the new password.
