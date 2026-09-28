# de.bloat

A small Android utility for cleaning up selected Samsung One UI packages using Shizuku.

## What it does

- Scans for a curated list of Samsung/One UI packages.
- Lets you select packages individually.
- Uses Shizuku for package-manager access.
- Disables selected packages for the current user.
- Does not delete system APK files.
- Builds a debug APK automatically with GitHub Actions.

## Safety

de.bloat deliberately does not run a blanket "delete Samsung stuff" command. Only packages in the built-in candidate list are shown.

The first version uses:

    pm disable-user --user 0 <package>

This is reversible compared with deleting the system package itself. The package list is intentionally conservative; critical Android, SystemUI, Settings, launcher, telephony, and core framework packages are excluded.

Only use de.bloat on a device you are authorized to modify.

## Requirements

- Android 6.0+
- Shizuku installed and running
- Shizuku permission granted to de.bloat
- A Samsung device is recommended for the bundled package list

## Build

The project uses Android Gradle Plugin 9.3.0 and Gradle 9.5.0.

    gradle :app:assembleDebug

The APK is produced at:

    app/build/outputs/apk/debug/app-debug.apk

## GitHub Actions

Pushes to main, pull requests, and manual workflow runs build the debug APK.

The workflow uploads an artifact named de.bloat-debug.

## License

Apache License 2.0.

Copyright 2026 Habib Batista.

## Status

Early development. The package database, restore UI, One UI detection, and polished UI are planned next.
