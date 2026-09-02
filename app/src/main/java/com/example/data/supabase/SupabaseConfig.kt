package com.example.data.supabase

import android.util.Log
import com.example.BuildConfig

/**
 * Supabase configuration credentials and endpoint constants.
 * Injects keys via BuildConfig from .env / Secrets Gradle Plugin,
 * with safe fallback values to the configured project instance.
 */
object SupabaseConfig {
    private const val TAG = "SupabaseConfig"

    private const val DEFAULT_URL = "https://hyjvgfdboujasjvqqjdz.supabase.co"
    private const val DEFAULT_KEY = "sb_publishable_iuX0gv36tHcVH8NgLSkYgg_jUg8fLQJ"

    val url: String = try {
        val buildUrl = BuildConfig.SUPABASE_URL
        if (buildUrl.isNullOrBlank()) DEFAULT_URL else buildUrl.trimEnd('/')
    } catch (e: Throwable) {
        DEFAULT_URL
    }

    val publishableKey: String = try {
        val buildKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        if (buildKey.isNullOrBlank()) {
            val legacyKey = BuildConfig.SUPABASE_KEY
            if (legacyKey.isNullOrBlank()) DEFAULT_KEY else legacyKey
        } else {
            buildKey
        }
    } catch (e: Throwable) {
        DEFAULT_KEY
    }

    val restUrl: String get() = "$url/rest/v1"
    val authUrl: String get() = "$url/auth/v1"
    val storageUrl: String get() = "$url/storage/v1"
    val realtimeUrl: String get() = "$url/realtime/v1"

    val isConfigured: Boolean
        get() = url.isNotBlank() && publishableKey.isNotBlank()

    init {
        Log.d(TAG, "Supabase configured with URL: $url (Key present: ${publishableKey.isNotBlank()})")
    }
}
