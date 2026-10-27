package com.moovieflix.app.server

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * Answers LAN discovery broadcasts from the Moovie Flix TV app.
 * TV sends "MOOVIEFLIX_DISCOVER_V1" to UDP 255.255.255.255:47630; we reply with the HTTP port.
 * The reply carries no secrets: pairing still needs the code shown on the PC.
 */
class MoovieFlixLanDiscovery(private val httpPort: Int, private val udpPort: Int = DISCOVERY_PORT) {
    @Volatile private var socket: DatagramSocket? = null

    fun start() {
        val s = runCatching { DatagramSocket(null).apply { reuseAddress = true; broadcast = true; bind(InetSocketAddress(udpPort)) } }.getOrNull() ?: return
        socket = s
        Thread({
            val buf = ByteArray(512)
            while (!s.isClosed) {
                runCatching {
                    val p = DatagramPacket(buf, buf.size)
                    s.receive(p)
                    val msg = String(p.data, 0, p.length, Charsets.UTF_8).trim()
                    if (msg.startsWith(PROBE)) {
                        val name = runCatching { java.net.InetAddress.getLocalHost().hostName }.getOrNull()?.replace("\"", "")?.take(40) ?: "PC"
                        val reply = """{"service":"moovieflix","version":1,"port":$httpPort,"name":"$name"}""".toByteArray()
                        s.send(DatagramPacket(reply, reply.size, p.socketAddress))
                    }
                }
            }
        }, "moovieflix-discovery").apply { isDaemon = true; start() }
    }

    fun stop() { socket?.close() }

    companion object {
        const val DISCOVERY_PORT = 47630
        const val PROBE = "MOOVIEFLIX_DISCOVER_V1"
    }
}
