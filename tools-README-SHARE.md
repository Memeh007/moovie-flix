# Moovie Flix (desktop + Android TV)

Moovie Flix is a fork of NuvioDesktop / NuvioTV (GPL-3.0). This package needs **no Docker and no
server setup**: the desktop app has a small sync server built in, and the TV app finds it on your
home network.

Every install is private. On first launch the PC app creates its own random account, keys and
passwords in `%APPDATA%\MoovieFlix\server\`. Nothing points at anybody else's computer, and there
are no built-in accounts or update servers.

## Install (normal users)

1. **PC:** run `MoovieFlix-Windows-x64.msi`, then open Moovie Flix. It signs in by itself.
   If Windows Firewall asks, allow **Private networks**. The TV needs this to reach the PC.
2. **TV (Android TV / Google TV / Fire TV):** install `MoovieFlix-TV.apk` (sideload it with
   a USB stick, "Downloader", or `adb install`). Open it.
3. On the PC, open **My Movies -> Pair a TV**. A 6-digit code appears.
4. The TV looks for the PC on its own. Enter the code and press **Pair**. That's it.
   - If the TV can't find the PC, type the address shown next to the code on the PC
     (e.g. `192.168.0.20:47630`).
   - The code changes after each use and expires after 15 minutes.
5. Done. Everything on your PC account comes over to the TV right after pairing and keeps syncing.

## What syncs (PC <-> TV)

Pairing signs the TV in to **the same account** as the PC app (the private one the PC created for
you), so the TV gets everything that account has, and both apps keep pulling each other's changes:

- **Addons** (with order, enabled/disabled and custom names) and **plugins / scraper repos**
- **Profiles** (names, avatars, colours, PINs) and per-profile **settings** (Nuvio keeps TV and PC playback/app settings separate, as upstream does)
- **Library**, **watched items**, **watch progress** and **Continue Watching**
- **Collections**, **home rows / catalog settings**, **media servers**, **debrid / provider keys**, tracker settings
- **My Movies -> Share to TV** folders (they show up on the TV by themselves)

Changes show up on the other device when it opens, comes back to the foreground, or within about
15 minutes while it stays open (there is no instant push). Reopening the TV app pulls right away.

**The PC app holds the data**, so it has to be running when the TV syncs. If the PC is off or
asleep, the TV keeps working with what it already has and signs back in by itself as soon as the PC
is back (no re-pairing). If your router gives the PC a new IP address, the TV finds it again on the
home network automatically.

**Back up** `%APPDATA%\MoovieFlix\server\` if you want to keep your account and data. Deleting that
folder creates a new, empty account; TVs then have to be paired again.

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
