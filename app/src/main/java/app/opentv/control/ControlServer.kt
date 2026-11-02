/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.control

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import kotlin.concurrent.thread
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject

/** Private-LAN, bearer-authenticated status and stop endpoints for a paired controller. */
object ControlServer {
    private const val PREFERRED_PORT = 8099
    private const val MAX_HEADER_BYTES = 8 * 1024
    private const val MAX_REQUEST_LINE = 2 * 1024

    @Volatile private var listener: ServerSocket? = null
    @Volatile private var bearer: String? = null
    @Volatile private var boundPort = 0

    data class PairInfo(val port: Int, val token: String)

    @Synchronized
    fun start(context: Context) {
        if (listener?.isClosed == false) return
        val token = runCatching { ControlCredentials.getOrCreate(context) }.getOrNull() ?: return
        val socket = runCatching { ServerSocket(PREFERRED_PORT) }.getOrElse {
            runCatching { ServerSocket(0) }.getOrNull() ?: return
        }
        listener = socket
        bearer = token
        boundPort = socket.localPort
        thread(name = "opentv-control", isDaemon = true) {
            while (!socket.isClosed) {
                val client = try { socket.accept() } catch (_: Exception) { break }
                runCatching { handle(client) }
                runCatching { client.close() }
            }
        }
    }

    fun pairInfo(): PairInfo? {
        val token = bearer ?: return null
        val port = boundPort.takeIf { it > 0 } ?: return null
        return PairInfo(port, token)
    }

    private fun handle(client: Socket) {
        client.soTimeout = 2_000
        if (!isPrivate(client.inetAddress?.hostAddress)) {
            respond(client, 403, "Forbidden", "{}")
            return
        }
        val input = client.getInputStream()
        val request = readLine(input, MAX_REQUEST_LINE) ?: return
        val parts = request.split(' ')
        if (parts.size < 2) { respond(client, 400, "Bad Request", "{}"); return }
        val method = parts[0].uppercase()
        val path = parts[1].substringBefore('?')
        val headers = HashMap<String, String>()
        var consumed = request.length
        while (true) {
            val line = readLine(input, MAX_HEADER_BYTES) ?: return
            consumed += line.length + 2
            if (consumed > MAX_HEADER_BYTES) { respond(client, 431, "Request Header Fields Too Large", "{}"); return }
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon > 0) headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
        }
        if (headers["content-length"]?.toIntOrNull()?.let { it > 0 } == true) {
            respond(client, 413, "Payload Too Large", "{}")
            return
        }
        val supplied = headers["authorization"].orEmpty().takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter(' ').orEmpty()
        val expected = bearer.orEmpty()
        val matches = supplied.length == expected.length && MessageDigest.isEqual(
            supplied.toByteArray(Charsets.UTF_8), expected.toByteArray(Charsets.UTF_8),
        )
        if (!matches) { respond(client, 404, "Not Found", "{}"); return }

        when {
            method == "GET" && path == "/control/v1/status" ->
                respond(client, 200, "OK", statusJson(PlaybackControlState.snapshot.value))
            method == "POST" && path == "/control/v1/stop" -> {
                PlaybackControlState.stop()
                respond(client, 202, "Accepted", "{\"status\":\"stopping\"}")
            }
            else -> respond(client, 404, "Not Found", "{}")
        }
    }

    internal fun statusJson(status: PlaybackControlSnapshot): String = buildJsonObject {
        put("kind", status.kind)
        put("source_id", status.sourceId)
        put("provider_id", status.providerId)
        put("state", status.state)
        put("position_ms", status.positionMs)
        put("video_frames", status.videoFrames?.let { JsonPrimitive(it) } ?: JsonNull)
        put("audio_language", status.audioLanguage?.let { JsonPrimitive(it) } ?: JsonNull)
        put("english_status", status.englishStatus)
        put("recovery_exhausted", status.recoveryExhausted)
        put("selection_error", status.selectionError?.let { JsonPrimitive(it) } ?: JsonNull)
    }.toString()

    private fun readLine(input: InputStream, limit: Int): String? {
        val bytes = ByteArrayOutputStream()
        while (bytes.size() <= limit) {
            val value = input.read()
            if (value < 0) return if (bytes.size() == 0) null else bytes.toString(Charsets.UTF_8.name())
            if (value == '\n'.code) break
            if (value != '\r'.code) bytes.write(value)
        }
        if (bytes.size() > limit) throw IllegalArgumentException("request line too large")
        return bytes.toString(Charsets.UTF_8.name())
    }

    private fun isPrivate(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val raw = host.substringBefore('%')
        val bytes = runCatching { java.net.InetAddress.getByName(raw).address }.getOrNull() ?: return false
        return when (bytes.size) {
            4 -> {
                val a = bytes[0].toInt() and 0xff
                val b = bytes[1].toInt() and 0xff
                a == 10 || (a == 192 && b == 168) || (a == 172 && b in 16..31) ||
                    (a == 169 && b == 254) || (a == 100 && b in 64..127) || a == 127
            }
            16 -> (bytes[0].toInt() and 0xfe) == 0xfc ||
                ((bytes[0].toInt() and 0xff) == 0xfe && (bytes[1].toInt() and 0xc0) == 0x80) ||
                bytes.all { it == 0.toByte() } || bytes.dropLast(1).all { it == 0.toByte() } && bytes.last() == 1.toByte()
            else -> false
        }
    }

    private fun respond(client: Socket, code: Int, reason: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $code $reason\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Cache-Control: no-store\r\nConnection: close\r\n\r\n"
        client.getOutputStream().apply { write(headers.toByteArray(Charsets.US_ASCII)); write(bytes); flush() }
    }
}
