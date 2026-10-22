# R8 rules for Expent's release build.
# Libraries (Retrofit, OkHttp, Room, Hilt, WorkManager, Firebase, Compose, Lottie) ship their own
# consumer rules; this file only covers what they can't know about.

# ---- Crash reports ----
# Readable stack traces in Crashlytics (the Crashlytics Gradle plugin uploads the mapping file)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Gson ----
# Gson reads/writes fields by name via reflection, and needs generic signatures for List<T> etc.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken

# Everything serialized with Gson: API request/response DTOs, models stored in prefs (User), and the
# pending_sync payloads. Field names must survive, or JSON keys silently stop matching.
-keep class com.aditya.expent.data.remote.dto.** { *; }
-keep class com.aditya.expent.domain.model.** { *; }
-keepclassmembers enum com.aditya.expent.data.local.entity.SyncStatus { *; }

# ---- Credential Manager (Google Sign-In) ----
# The Play Services provider is loaded by reflection
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
