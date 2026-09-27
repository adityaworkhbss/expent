package com.aditya.expent.utils

import android.util.Log

object AppLogger {

    private const val TAG_API = "EXPENT_API"
    private const val TAG_ROOM = "EXPENT_ROOM"
    private const val TAG_SYNC = "EXPENT_SYNC"
    private const val MAX_LOG_LENGTH = 3500

    // =========================================================================
    // API LOGGING
    // =========================================================================

    fun apiRequest(method: String, url: String, headers: Map<String, String>? = null, payload: String? = null) {
        val builder = StringBuilder()
        builder.append("[API] [REQUEST] --> $method $url\n")
        if (!headers.isNullOrEmpty()) {
            builder.append("Headers:\n")
            headers.forEach { (k, v) ->
                val maskedVal = if (k.equals("Authorization", ignoreCase = true) && v.startsWith("Bearer ")) {
                    "Bearer " + v.removePrefix("Bearer ").take(8) + "..."
                } else {
                    v
                }
                builder.append("  $k: $maskedVal\n")
            }
        }
        if (!payload.isNullOrBlank()) {
            builder.append("Request Payload:\n$payload")
        } else {
            builder.append("Request Payload: (empty)")
        }
        logChunked(TAG_API, Log.INFO, builder.toString())
    }

    fun apiResponse(method: String, url: String, code: Int, durationMs: Long? = null, payload: String? = null) {
        val builder = StringBuilder()
        val durationStr = durationMs?.let { " (${it}ms)" } ?: ""
        builder.append("[API] [RESPONSE] <-- $code $method $url$durationStr\n")
        if (!payload.isNullOrBlank()) {
            builder.append("Response Payload:\n$payload")
        } else {
            builder.append("Response Payload: (empty)")
        }
        val priority = if (code in 200..299) Log.INFO else Log.WARN
        logChunked(TAG_API, priority, builder.toString())
    }

    fun apiError(method: String, url: String, error: String?, durationMs: Long? = null, throwable: Throwable? = null) {
        val durationStr = durationMs?.let { " (${it}ms)" } ?: ""
        val message = "[API] [ERROR] <-- FAILED $method $url$durationStr: ${error ?: "Unknown error"}"
        logChunked(TAG_API, Log.ERROR, message, throwable)
    }

    // =========================================================================
    // ROOM LOGGING
    // =========================================================================

    fun room(
        operation: String,
        table: String,
        payload: Any? = null,
        result: Any? = null
    ) {
        val payloadStr = formatData(payload)
        val resultStr = formatData(result)
        val message = "[ROOM] [$operation] Table: $table | Payload: $payloadStr | Result: $resultStr"
        logChunked(TAG_ROOM, Log.INFO, message)
    }

    fun roomQuery(sql: String, bindArgs: List<Any?>?) {
        val cleanSql = sql.trim().replace("\n", " ").replace(Regex("\\s+"), " ")
        val argsStr = bindArgs?.joinToString(", ", "[", "]") ?: "[]"
        val message = "[ROOM] [QUERY] SQL: $cleanSql | Args: $argsStr"
        logChunked(TAG_ROOM, Log.DEBUG, message)
    }

    // =========================================================================
    // SYNC LOGGING
    // =========================================================================

    fun sync(action: String, details: String) {
        val message = "[SYNC] [$action] $details"
        logChunked(TAG_SYNC, Log.INFO, message)
    }

    fun syncError(action: String, details: String, throwable: Throwable? = null) {
        val message = "[SYNC] [ERROR] [$action] $details"
        logChunked(TAG_SYNC, Log.ERROR, message, throwable)
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private fun formatData(data: Any?): String {
        return when (data) {
            null -> "None"
            is String -> data
            is List<*> -> "List(size=${data.size}): $data"
            else -> data.toString()
        }
    }

    private fun logChunked(tag: String, priority: Int, message: String, throwable: Throwable? = null) {
        if (message.length <= MAX_LOG_LENGTH) {
            Log.println(priority, tag, message)
            throwable?.let { Log.println(priority, tag, Log.getStackTraceString(it)) }
            return
        }

        var i = 0
        val len = message.length
        while (i < len) {
            val end = (i + MAX_LOG_LENGTH).coerceAtMost(len)
            Log.println(priority, tag, message.substring(i, end))
            i = end
        }
        throwable?.let { Log.println(priority, tag, Log.getStackTraceString(it)) }
    }
}
