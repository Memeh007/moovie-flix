# Moovie Flix (desktop + Android TV)

Moovie Flix is a fork of NuvioDesktop / NuvioTV (GPL-3.0). This package needs **no Docker and no
server setup**: the desktop app has a small sync server built in, and the TV app finds it on your
home network.

Every install is private. On first launch the PC app creates its own random account, keys and
passwords in `%APPDATA%\MoovieFlix\server\`. Nothing points at anybody else's computer, and there
are no built-in accounts or update servers.

## Install (normal users)

1. **PC:** run `MoovieFlix-share-*.msi`, then open Moovie Flix. It signs in by itself.
   If Windows Firewall asks, allow **Private networks**. The TV needs this to reach the PC.
2. **TV (Android TV / Google TV / Fire TV):** install `MoovieFlix-TV-share.apk` (sideload it with
   a USB stick, "Downloader", or `adb install`). Open it.
3. On the PC, open **My Movies -> Pair a TV**. A 6-digit code appears.
4. The TV looks for the PC on its own. Enter the code and press **Pair**. That's it.
   - If the TV can't find the PC, type the address shown next to the code on the PC
     (e.g. `192.168.0.20:47630`).
   - The code changes after each use and expires after 15 minutes.
5. Addons, profiles, library, watch progress and debrid settings now sync between the PC and the TV
   while the PC app is running. "Share to TV" in My Movies streams your own movie folders to the TV.

Keep the PC app open when you use the TV, because it holds your synced data.
**Back up** `%APPDATA%\MoovieFlix\server\` if you want to keep your account and data.

To re-pair a TV, clear the app's data on the TV (Settings -> Apps -> Moovie Flix -> Clear data)
and pair again. Each pairing gets its own password, so you can revoke one TV without affecting the others.

## Build it yourself

Requirements: JDK 17, Android SDK (for the TV app), Windows for the MSI.

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
