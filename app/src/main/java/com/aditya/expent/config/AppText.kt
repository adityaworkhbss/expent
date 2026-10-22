package com.aditya.expent.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object AppText {

    private var content by mutableStateOf<JsonObject?>(null)

    internal fun update(newContent: JsonObject?) {
        content = newContent
    }

    fun get(path: String, fallback: String, vararg args: Pair<String, Any?>): String {
        val value = find(path)
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString
            ?: fallback
        return format(value, args)
    }

    fun list(path: String, fallback: List<String>): List<String> {
        val array = find(path)?.takeIf { it.isJsonArray }?.asJsonArray ?: return fallback
        val values = array.mapNotNull { el -> el.takeIf { it.isJsonPrimitive }?.asString }
        return values.ifEmpty { fallback }
    }

    private fun find(path: String): JsonElement? {
        var node: JsonElement? = content ?: return null
        for (key in path.split('.')) {
            node = (node as? JsonObject)?.get(key) ?: return null
        }
        return node
    }

    private fun format(template: String, args: Array<out Pair<String, Any?>>): String {
        if (args.isEmpty()) return template
        var result = template
        for ((key, value) in args) {
            result = result.replace("{$key}", value?.toString().orEmpty())
        }
        return result
    }
}
