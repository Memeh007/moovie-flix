package com.moovieflix.app.server

import java.io.File

/**
 * Headless runner for testing the embedded sync server without the UI:
 *   gradlew :composeApp:runSyncServerHeadless -PserverDir=C:\temp\mf-test
 * Prints the URL, anon key, owner email and current pairing code, then serves until killed.
 */
fun main(args: Array<String>) {
    val dir = File(args.firstOrNull() ?: System.getProperty("java.io.tmpdir") + "/moovieflix-server-test")
    val server = MoovieFlixSyncServer(dir)
    val port = server.start(listOf(args.getOrNull(1)?.toIntOrNull() ?: 47630, 0))
    MoovieFlixLanDiscovery(port).start()
    println("MOOVIEFLIX_SERVER_URL=${server.localUrl}")
    println("MOOVIEFLIX_ANON_KEY=${server.anonKey}")
    println("MOOVIEFLIX_OWNER_EMAIL=${server.ownerEmail}")
    println("MOOVIEFLIX_OWNER_PASSWORD=${server.ownerPassword}")
    println("MOOVIEFLIX_PAIR_CODE=${server.pairingCode()}")
    println("MOOVIEFLIX_DATA_DIR=${dir.absolutePath}")
    System.out.flush()
    val codeFile = File(dir, "pair-code.txt")
    while (true) { codeFile.writeText(server.pairingCode()); Thread.sleep(2000) }
}
