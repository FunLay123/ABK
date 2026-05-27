package com.abk.kernel.debug

import com.abk.kernel.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val INGEST_URL =
    "http://10.0.2.2:7289/ingest/7a3776f9-3d40-4eae-9f35-9d2a173edad5"
private const val SESSION_ID = "c5ed76"

// #region agent log
object DebugSessionLog {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun log(location: String, message: String, data: Map<String, Any?>, hypothesisId: String) {
        if (!BuildConfig.DEBUG) return
        val payload = buildString {
            append("{\"sessionId\":\"")
            append(SESSION_ID)
            append("\",\"hypothesisId\":\"")
            append(hypothesisId)
            append("\",\"location\":\"")
            append(location.replace("\"", "\\\""))
            append("\",\"message\":\"")
            append(message.replace("\"", "\\\""))
            append("\",\"data\":{")
            append(
                data.entries.joinToString(",") { (k, v) ->
                    "\"$k\":${valueJson(v)}"
                }
            )
            append("},\"timestamp\":")
            append(System.currentTimeMillis())
            append("}")
        }
        scope.launch {
            runCatching {
                val conn = (URL(INGEST_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Debug-Session-Id", SESSION_ID)
                    doOutput = true
                    connectTimeout = 1500
                    readTimeout = 1500
                }
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                conn.inputStream.use { it.readBytes() }
                conn.disconnect()
            }
        }
    }

    private fun valueJson(value: Any?): String = when (value) {
        null -> "null"
        is Number, is Boolean -> value.toString()
        else -> "\"${value.toString().replace("\"", "\\\"")}\""
    }
}
// #endregion
