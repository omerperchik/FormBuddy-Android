package com.formbuddy.android.data.repository

import android.util.Log
import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.data.remote.firebase.FirebaseManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sub-second fill for popular forms.
 *
 * The flow:
 *   1. SHA-256 the input PDF bytes locally.
 *   2. Look up `formTemplates/{sha256}` in Firestore (the index is built by
 *      our ingestion pipeline + community uploads). The doc stores the
 *      pre-mapped JSON.
 *   3. If a hit, deserialize into a [FormTemplate] and skip the whole
 *      analysis pipeline (Replicate / OCR / inference). Latency: ~300 ms
 *      vs 30-90 s for a fresh scan.
 *
 * Hits are cached on disk so the second open of the same form is offline.
 */
@Singleton
class InstantMatchRepository @Inject constructor(
    @ApplicationContext private val appContext: android.content.Context,
    private val firebaseManager: FirebaseManager,
    private val gson: Gson
) {
    private val cacheDir: File by lazy {
        File(appContext.cacheDir, "instant-match").apply { mkdirs() }
    }

    suspend fun findByPdfBytes(pdfBytes: ByteArray): FormTemplate? = withContext(Dispatchers.IO) {
        val sha = sha256Hex(pdfBytes)
        // 1. Local cache — survives offline + repeated opens.
        readCached(sha)?.let { return@withContext it }
        // 2. Firestore lookup.
        val cloud = runCatching {
            firebaseManager.fetchInstantMatch(sha)
        }.onFailure { Log.w(TAG, "instant-match lookup failed: ${it.message}") }.getOrNull()
            ?: return@withContext null
        writeCached(sha, cloud)
        return@withContext deserialize(cloud)
    }

    private fun deserialize(json: String): FormTemplate? = try {
        gson.fromJson(json, FormTemplate::class.java)
    } catch (_: JsonSyntaxException) { null }

    private fun readCached(sha: String): FormTemplate? {
        val f = File(cacheDir, "$sha.json")
        if (!f.exists() || f.length() == 0L) return null
        return runCatching { deserialize(f.readText()) }.getOrNull()
    }

    private fun writeCached(sha: String, json: String) {
        runCatching { File(cacheDir, "$sha.json").writeText(json) }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    companion object { private const val TAG = "InstantMatchRepository" }
}
