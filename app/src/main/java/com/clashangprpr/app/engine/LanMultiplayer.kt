package com.clashangprpr.app.engine

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Local Wi-Fi & Mobile Hotspot Co-Op Multiplayer Engine (بدون إنترنت خارجي):
 * - Automatic UDP LAN/Hotspot Room Discovery on port 41777
 * - 1-Tap Hotspot Gateway Connect & Short Room Code / Direct IP Connect on TCP port 41778
 * - Non-blocking 60 FPS packet queues for real-time position, block building, farming, combat & world state sync.
 */
object LanMultiplayer {
    const val UDP_PORT = 41777
    const val TCP_PORT = 41778

    enum class Role { NONE, HOST, CLIENT }

    data class DiscoveredRoom(
        val hostName: String,
        val ip: String,
        val worldModeAr: String,
        val dayNo: Int,
        var lastSeenMs: Long
    )

    @Volatile var role: Role = Role.NONE
    @Volatile var isConnected: Boolean = false
    @Volatile var statusAr: String = "غير متصل — اختر استضافة غرفة أو الانضمام لصديقك عبر الواي فاي / نقطة الاتصال"
    @Volatile var partnerName: String = "صديقك الفارس"
    @Volatile var hostNewWorldMode: Boolean = false

    val discoveredRooms = CopyOnWriteArrayList<DiscoveredRoom>()
    val incoming = ConcurrentLinkedQueue<String>()
    private val outgoing = ConcurrentLinkedQueue<String>()

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var udpListenSocket: DatagramSocket? = null
    @Volatile private var discoveryRunning = false
    @Volatile private var sessionId = 0

    fun startDiscoveryListener() {
        if (discoveryRunning) return
        discoveryRunning = true
        Thread {
            var sock: DatagramSocket? = null
            try {
                sock = DatagramSocket(null)
                sock.reuseAddress = true
                sock.bind(InetSocketAddress(UDP_PORT))
                udpListenSocket = sock
                val buf = ByteArray(512)
                while (discoveryRunning) {
                    val pkt = DatagramPacket(buf, buf.size)
                    sock.receive(pkt)
                    val msg = String(pkt.data, 0, pkt.length, Charsets.UTF_8).trim()
                    if (msg.startsWith("KP_ROOM|")) {
                        val parts = msg.split("|")
                        if (parts.size >= 5) {
                            val hName = parts[1]
                            val ip = pkt.address?.hostAddress ?: parts[2]
                            val wMode = parts[3]
                            val dNo = parts[4].toIntOrNull() ?: 1
                            val myIp = getLocalIpAddress()
                            if (ip != myIp || role == Role.NONE) {
                                val now = System.currentTimeMillis()
                                val existing = discoveredRooms.firstOrNull { it.ip == ip }
                                if (existing != null) {
                                    discoveredRooms.remove(existing)
                                }
                                discoveredRooms.add(DiscoveredRoom(hName, ip, wMode, dNo, now))
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try { sock?.close() } catch (_: Throwable) {}
                discoveryRunning = false
            }
        }.apply { isDaemon = true; start() }
    }

    fun pruneRooms() {
        val now = System.currentTimeMillis()
        for (r in discoveredRooms) {
            if (now - r.lastSeenMs > 6500L) discoveredRooms.remove(r)
        }
    }

    /** Finds the device's active Wi-Fi or Mobile Hotspot IPv4 address (e.g. 192.168.43.1 or 192.168.1.x). */
    fun getLocalIpAddress(): String {
        var fallback = "192.168.43.1"
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return fallback
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                val name = nif.name.lowercase()
                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (name.contains("ap") || name.contains("wlan") || name.contains("swlan") || name.contains("rndis")) {
                            return ip
                        }
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            fallback = ip
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        return fallback
    }

    /** Short 2-number Room Code derived from the last 2 octets of the Host IP (e.g. 192.168.43.1 -> "43.1"). */
    fun getRoomCode(): String {
        val ip = getLocalIpAddress()
        val p = ip.split(".")
        return if (p.size == 4) "${p[2]}.${p[3]}" else ip
    }

    /** Resolves either a short room code ("43.1" / "1.15") or full IP into a target IPv4 address. */
    fun resolveInputToIp(input: String): String {
        val clean = input.trim().replace('-', '.').replace(' ', '.')
        if (clean.isEmpty()) {
            val firstRoom = discoveredRooms.firstOrNull()
            if (firstRoom != null) return firstRoom.ip
            val myIp = getLocalIpAddress()
            val p = myIp.split(".")
            return if (p.size == 4) "${p[0]}.${p[1]}.${p[2]}.1" else "192.168.43.1"
        }
        val parts = clean.split(".").filter { it.isNotEmpty() }
        if (parts.size == 4) return parts.joinToString(".")
        val myIp = getLocalIpAddress()
        val mp = myIp.split(".")
        val prefixA = if (mp.size == 4) mp[0] else "192"
        val prefixB = if (mp.size == 4) mp[1] else "168"
        val prefixC = if (mp.size == 4) mp[2] else "43"
        return when (parts.size) {
            1 -> "$prefixA.$prefixB.$prefixC.${parts[0]}"
            2 -> "$prefixA.$prefixB.${parts[0]}.${parts[1]}"
            else -> clean
        }
    }

    fun startHosting(hostDisplayName: String, newCoopWorld: Boolean, dayProvider: () -> Int, onClientJoined: () -> Unit) {
        stopSession()
        startDiscoveryListener()
        val mySession = ++sessionId
        role = Role.HOST
        hostNewWorldMode = newCoopWorld
        val modeLabel = if (newCoopWorld) "عالم تعاوني جديد" else "عالم المضيف الحالي"
        val myIp = getLocalIpAddress()
        statusAr = "بانتظار صديقك... الكود السريع: (${getRoomCode()}) أو IP: $myIp"

        // Thread 1: UDP Broadcast Beacon so friend sees room automatically on Wi-Fi / Hotspot
        Thread {
            var bSock: DatagramSocket? = null
            try {
                bSock = DatagramSocket()
                bSock.broadcast = true
                while (sessionId == mySession && role == Role.HOST && !isConnected) {
                    val curIp = getLocalIpAddress()
                    val payload = "KP_ROOM|$hostDisplayName|$curIp|$modeLabel|${dayProvider()}"
                    val bytes = payload.toByteArray(Charsets.UTF_8)
                    val targets = HashSet<String>()
                    targets.add("255.255.255.255")
                    targets.add("192.168.43.255")
                    targets.add("192.168.137.255")
                    val oct = curIp.split(".")
                    if (oct.size == 4) targets.add("${oct[0]}.${oct[1]}.${oct[2]}.255")
                    for (t in targets) {
                        try {
                            val addr = InetAddress.getByName(t)
                            bSock.send(DatagramPacket(bytes, bytes.size, addr, UDP_PORT))
                        } catch (_: Throwable) {}
                    }
                    Thread.sleep(900L)
                }
            } catch (_: Throwable) {
            } finally {
                try { bSock?.close() } catch (_: Throwable) {}
            }
        }.apply { isDaemon = true; start() }

        // Thread 2: TCP Server Socket accepting friend's connection
        Thread {
            try {
                val srv = ServerSocket()
                srv.reuseAddress = true
                srv.bind(InetSocketAddress(TCP_PORT))
                serverSocket = srv
                val client = srv.accept()
                if (sessionId != mySession) {
                    client.close()
                    return@Thread
                }
                client.tcpNoDelay = true
                activeSocket = client
                isConnected = true
                statusAr = "متصل الآن مع صديقك في العالم المفتوح! 🤝"
                onClientJoined()
                runSocketLoops(client, mySession)
            } catch (e: Throwable) {
                if (sessionId == mySession) {
                    statusAr = "توقف الاستضافة أو تعذر فتح المنفذ"
                    role = Role.NONE
                    isConnected = false
                }
            }
        }.apply { isDaemon = true; start() }
    }

    fun connectToHost(targetIpOrCode: String, onConnected: () -> Unit) {
        stopSession()
        startDiscoveryListener()
        val mySession = ++sessionId
        role = Role.CLIENT
        val resolvedIp = resolveInputToIp(targetIpOrCode)
        statusAr = "جاري الاتصال بصديقك على $resolvedIp ..."

        Thread {
            val candidates = LinkedHashSet<String>()
            candidates.add(resolvedIp)
            for (r in discoveredRooms) candidates.add(r.ip)
            val myIp = getLocalIpAddress()
            val p = myIp.split(".")
            if (p.size == 4) candidates.add("${p[0]}.${p[1]}.${p[2]}.1")
            candidates.add("192.168.43.1")
            candidates.add("192.168.137.1")

            var connectedSock: Socket? = null
            for (candIp in candidates) {
                if (sessionId != mySession) return@Thread
                try {
                    val s = Socket()
                    s.connect(InetSocketAddress(candIp, TCP_PORT), 2200)
                    s.tcpNoDelay = true
                    connectedSock = s
                    break
                } catch (_: Throwable) {}
            }

            if (connectedSock != null && sessionId == mySession) {
                activeSocket = connectedSock
                isConnected = true
                statusAr = "تم الاتصال بصديقك بنجاح! استعد للمغامرة التعاونية 🤝"
                onConnected()
                runSocketLoops(connectedSock, mySession)
            } else if (sessionId == mySession) {
                role = Role.NONE
                isConnected = false
                statusAr = "لم يتم العثور على الغرفة ($resolvedIp) — تأكد أن صديقك ضغط (استضافة) على نفس الواي فاي أو نقطة الاتصال"
            }
        }.apply { isDaemon = true; start() }
    }

    private fun runSocketLoops(sock: Socket, mySession: Int) {
        // Writer thread
        Thread {
            try {
                val out = PrintWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8), true)
                while (sessionId == mySession && isConnected && !sock.isClosed) {
                    val msg = outgoing.poll()
                    if (msg != null) {
                        out.println(msg)
                    } else {
                        Thread.sleep(12L)
                    }
                }
            } catch (_: Throwable) {}
        }.apply { isDaemon = true; start() }

        // Reader loop
        try {
            val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
            while (sessionId == mySession && isConnected) {
                val line = reader.readLine() ?: break
                if (line.isNotEmpty()) incoming.add(line)
            }
        } catch (_: Throwable) {
        } finally {
            if (sessionId == mySession) {
                isConnected = false
                role = Role.NONE
                statusAr = "انقطع الاتصال مع الصديق"
                incoming.add("SYS_DISCONNECTED")
            }
            try { sock.close() } catch (_: Throwable) {}
        }
    }

    fun send(msg: String) {
        if (!isConnected) return
        if (outgoing.size < 400) {
            outgoing.add(msg)
        }
    }

    fun stopSession() {
        sessionId++
        isConnected = false
        role = Role.NONE
        outgoing.clear()
        incoming.clear()
        try { activeSocket?.close() } catch (_: Throwable) {}
        try { serverSocket?.close() } catch (_: Throwable) {}
        activeSocket = null
        serverSocket = null
        statusAr = "غير متصل — اختر استضافة غرفة أو الانضمام لصديقك"
    }
}
