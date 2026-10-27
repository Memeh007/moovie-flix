# Moovie Flix (desktop + Android TV)

> **Fork notice:** Moovie Flix is a modified fork of [NuvioDesktop / NuvioTV](https://github.com/tapframe) by
> Tapframe and contributors, licensed under **GPL-3.0** (see `LICENSE`). All credit for the original apps goes
> to the Nuvio / Tapframe authors. This repository contains no accounts, servers, keys or personal setup.

## Download

From [Releases](https://github.com/Memeh007/moovie-flix/releases/latest):

- **TV app:** `MoovieFlix-TV.apk` - direct link:
  `https://github.com/Memeh007/moovie-flix/releases/latest/download/MoovieFlix-TV.apk`
- **PC app (Windows x64):** `MoovieFlix-Windows-x64.msi`

### Installing on an Android TV / Google TV / Fire TV

1. On the TV install the free **Downloader** app (by AFTVnews) from the Play Store / Amazon Appstore.
2. Allow it to install unknown apps (TV Settings -> Apps -> Security / Special app access -> Install unknown apps -> Downloader).
3. In Downloader, type the APK link above and press Go, then Install.
   (Or copy the APK to a USB stick, or use `adb install MoovieFlix-TV.apk`.)

## Quick start (no server needed)

1. **PC:** run the MSI, open Moovie Flix. It creates its own private account on first launch
   (stored in `%APPDATA%\MoovieFlix\server\`). Allow **Private networks** if Windows Firewall asks.
2. **TV:** open Moovie Flix. It searches your home network for the PC.
3. **PC:** open **My Movies -> Pair a TV**. A 6-digit code appears.
4. **TV:** pick your PC, enter the code, press **Pair**. If the PC isn't found, type the address shown
   next to the code (for example `192.168.0.20:47630`).
5. Addons, profiles, library, progress and debrid settings now sync while the PC app is running.

## Already running your own Supabase backend?

The TV app can pair with a self-hosted backend too. Run `tools/pair-bridge.ps1` on any PC on the
same network, pointing it at a properties file with your backend URL (LAN address), anon key and account:

```
powershell -ExecutionPolicy Bypass -File tools\pair-bridge.ps1 -Config C:\path\to\local.properties
```

It shows a 6-digit code and answers the TV's discovery; after pairing the TV talks to your backend directly.
## Build it yourself

Requirements: JDK 17, Android SDK (for the TV app), Windows for the MSI.

Note: `libmpv-2.dll` (115 MB, over GitHub's file limit) is not in the repo. Download it from the release assets and place it in `MoovieFlix-desktop/composeApp/src/desktopMain/native/windows/runtime/` before building the Windows app.

```
cd MoovieFlix-desktop
copy local.properties.example local.properties   (edit sdk.dir)
gradlew :composeApp:packageReleaseMsi
-> composeApp\build\compose\binaries\main-release\msi\

cd MoovieFlix-tv
keytool -genkeypair -keystore C:\Users\YOU\.android\my-moovieflix.jks -alias moovieflix -keyalg RSA -keysize 4096 -validity 10000
copy local.properties.example local.properties   (edit sdk.dir + your key)
gradlew :app:assembleFullDebug
-> app\build\outputs\apk\full\debug\
```

Sign the TV app with **your own** key. Never publish a keystore or local.properties.

Test the sync server without the UI:
`gradlew :composeApp:runSyncServerHeadless -PserverDir=C:\temp\mf-test -PserverPort=47699`

## What the built-in server does and doesn't do

Supported: sign-in/refresh, profiles + PINs, addons, plugins, library, watch progress, watched
items, settings, home rows, collections, media servers, provider/debrid credentials, tracker
settings, linked devices, Moovie Flix shared folders, TV pairing.

Not supported (these turn off cleanly): realtime push (apps fall back to normal syncing), watch
party, QR / web sign-in, avatar uploads and the supporter catalog, account deletion from the app,
and using the server from outside your home network.

Advanced: if you run your own Supabase backend, set `MOOVIEFLIX_SUPABASE_URL` etc. in
local.properties. The built-in server then stays off and the TV skips pairing.

## Ports

TCP 47630 is the sync server (it falls back to 47640/47650 if that's busy). UDP 47630 is used for
TV discovery. 47631 is My Movies sharing (optional).

## License

GPL-3.0, same as upstream Nuvio. The source for these builds is in this package.

