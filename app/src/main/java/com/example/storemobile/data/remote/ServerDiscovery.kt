package com.example.storemobile.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Result of a successful auto-discovery scan. */
data class DiscoveryResult(val baseUrl: String, val ip: String)

/**
 * Finds the store server automatically on the local Wi-Fi network.
 *
 * How it works (light on the server, fast on the phone):
 *  1. Reads the phone's own private IPv4 (e.g. 192.168.10.204) and derives the
 *     /24 subnet prefix (192.168.10.).
 *  2. Scans every host .1–.254 on that subnet in parallel. For each host it
 *     first does a fast TCP probe on the server port; only hosts that actually
 *     have the port open get a single tiny HTTP request to confirm it is really
 *     our API (GET api/Users/sellers → 200). Dead hosts never receive HTTP.
 *  3. Returns the first confirmed server as a ready-to-save base URL
 *     (http://<ip>:<port>/). The remaining probes are cancelled immediately.
 *
 * No extra permissions are required — the local IP is read from
 * [NetworkInterface], which works on real devices and emulators alike.
 */
object ServerDiscovery {

    const val DEFAULT_PORT = 5050

    private const val MAX_PARALLEL = 64
    private const val TCP_TIMEOUT_MS = 300
    private const val OVERALL_TIMEOUT_MS = 25_000L

    suspend fun discover(
        port: Int = DEFAULT_PORT,
        onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> }
    ): DiscoveryResult? = withContext(Dispatchers.IO) {
        val prefixes = localIpv4Prefixes()
        if (prefixes.isEmpty()) return@withContext null

        // Candidate IPs across every detected /24 subnet.
        val candidates = buildList {
            prefixes.forEach { prefix ->
                for (host in 1..254) add(prefix + host)
            }
        }
        if (candidates.isEmpty()) return@withContext null

        val client = probeClient()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val result = CompletableDeferred<DiscoveryResult?>()
        val semaphore = Semaphore(MAX_PARALLEL)
        val counter = AtomicInteger(0)
        val total = candidates.size

        try {
            candidates.forEach { ip ->
                scope.launch {
                    if (!result.isCompleted) {
                        semaphore.withPermit {
                            if (!result.isCompleted && probe(ip, port, client) && !result.isCompleted) {
                                result.complete(DiscoveryResult("http://$ip:$port/", ip))
                            }
                        }
                    }
                    val done = counter.incrementAndGet()
                    onProgress(done, total)
                    if (done >= total && !result.isCompleted) result.complete(null)
                }
            }

            withTimeoutOrNull(OVERALL_TIMEOUT_MS) { result.await() }
        } finally {
            scope.cancel()
        }
    }

    /** Quick TCP knock, then a tiny HTTP request to confirm it's our API. */
    private fun probe(ip: String, port: Int, client: OkHttpClient): Boolean {
        // 1) Is the port even open? (fast, no HTTP overhead on dead hosts)
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), TCP_TIMEOUT_MS)
            }
        } catch (e: Exception) {
            return false
        }
        // 2) Confirm it responds like our store server.
        return try {
            val request = Request.Builder()
                .url("http://$ip:$port/api/Users/sellers")
                .get()
                .build()
            client.newCall(request).execute().use { resp -> resp.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    private fun probeClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.MILLISECONDS)
        .readTimeout(1200, TimeUnit.MILLISECONDS)
        .writeTimeout(1200, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    /** Private IPv4 /24 prefixes of every active interface, e.g. "192.168.10.". */
    private fun localIpv4Prefixes(): List<String> {
        val prefixes = LinkedHashSet<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && addr.isSiteLocalAddress) {
                        val ip = addr.hostAddress ?: continue
                        val lastDot = ip.lastIndexOf('.')
                        if (lastDot > 0) prefixes.add(ip.substring(0, lastDot + 1))
                    }
                }
            }
        } catch (e: Exception) {
            // ignore — fall back to empty (caller shows "not found")
        }
        return prefixes.toList()
    }
}
