package com.formbuddy.android.data.drive

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.formbuddy.android.data.privacy.PrivacyAuditLog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.driveStore by preferencesDataStore("drive_export")

/**
 * Auto-exports filled PDFs to the user's Google Drive (Pro feature).
 *
 * Wiring:
 *   1. User taps "Auto-export to Drive" in Settings (Pro-gated).
 *   2. Host activity launches a Google Sign-In intent with the
 *      `DriveScopes.DRIVE_FILE` scope. The selected account is stored.
 *   3. After every form save, [FillingViewModel] calls
 *      [exportPdfIfEnabled] with the just-rendered filled PDF. We upload
 *      it to a `FormBuddy/` folder (created lazily) under the user's Drive.
 *
 * The `DRIVE_FILE` scope is the narrowest one that still lets the user
 * download what FormBuddy uploaded — we never see anything else in their
 * Drive.
 */
@Singleton
class DriveExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auditLog: PrivacyAuditLog
) {

    val isEnabled: Flow<Boolean> = context.driveStore.data.map { it[KEY_ENABLED] ?: false }
    val accountEmail: Flow<String?> = context.driveStore.data.map { it[KEY_ACCOUNT] }

    suspend fun setEnabled(enabled: Boolean, accountEmail: String?) {
        context.driveStore.edit {
            it[KEY_ENABLED] = enabled
            if (accountEmail != null) it[KEY_ACCOUNT] = accountEmail
            else it.remove(KEY_ACCOUNT)
        }
    }

    /** Returns true if a sign-in is already valid and the toggle is on. */
    suspend fun isReady(): Boolean {
        val on = isEnabled.first()
        if (!on) return false
        val email = accountEmail.first() ?: return false
        return GoogleSignIn.getLastSignedInAccount(context)?.email == email
    }

    /** Uploads [pdfBytes] to the user's Drive under the FormBuddy folder. */
    suspend fun exportPdfIfEnabled(pdfBytes: ByteArray, displayName: String): String? {
        if (!isReady()) return null
        return withContext(Dispatchers.IO) {
            try {
                val drive = buildDriveClient() ?: return@withContext null
                val folderId = ensureFormBuddyFolder(drive)
                val file = DriveFile().apply {
                    name = "$displayName.pdf"
                    parents = listOf(folderId)
                    mimeType = "application/pdf"
                }
                val content = ByteArrayContent("application/pdf", pdfBytes)
                val uploaded = drive.files().create(file, content)
                    .setFields("id,name,webViewLink")
                    .execute()
                auditLog.log(
                    PrivacyAuditLog.Category.Cloud,
                    destination = "drive:files",
                    description = "Auto-exported '${file.name}' (${pdfBytes.size} bytes)"
                )
                uploaded.id
            } catch (t: Throwable) {
                Log.w(TAG, "Drive export failed", t)
                null
            }
        }
    }

    private fun buildDriveClient(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("FormBuddy").build()
    }

    /** Lazy-creates a `FormBuddy/` folder so all exports stay together. */
    private fun ensureFormBuddyFolder(drive: Drive): String {
        val list = drive.files().list()
            .setQ("mimeType='application/vnd.google-apps.folder' and name='FormBuddy' and trashed=false")
            .setFields("files(id,name)")
            .execute()
        val existing = list.files.firstOrNull()
        if (existing != null) return existing.id
        val meta = DriveFile().apply {
            name = "FormBuddy"
            mimeType = "application/vnd.google-apps.folder"
        }
        return drive.files().create(meta).setFields("id").execute().id
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("enabled")
        private val KEY_ACCOUNT = stringPreferencesKey("account_email")
        private const val TAG = "DriveExport"
    }
}
