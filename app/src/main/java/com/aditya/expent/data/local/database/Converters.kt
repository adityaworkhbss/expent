package com.aditya.expent.data.local.database

import androidx.room.TypeConverter
import com.aditya.expent.data.local.entity.SyncStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? {
        return value?.let {
            try {
                SyncStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                SyncStatus.SYNCED
            }
        }
    }
}

