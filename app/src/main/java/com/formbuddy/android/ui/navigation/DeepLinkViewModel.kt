package com.formbuddy.android.ui.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.repository.FormRepository
import com.formbuddy.android.data.share.ReferralService
import com.formbuddy.android.data.share.ShareForFillService
import com.formbuddy.android.data.telemetry.Analytics
import com.formbuddy.android.data.telemetry.Events
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Routes deep-link events to the right service.
 *   - Referral: claim 30 days Pro for the recipient.
 *   - Share-for-fill: download the shared template, save it as a fresh
 *     local form, hand the new local URI back to the navigator so the
 *     filling screen opens the form.
 */
@HiltViewModel
class DeepLinkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val referralService: ReferralService,
    private val shareForFillService: ShareForFillService,
    private val formRepository: FormRepository,
    private val gson: Gson,
    private val analytics: Analytics
) : ViewModel() {

    fun consumeReferral(referrerId: String, planCode: String) {
        viewModelScope.launch {
            referralService.claimReferral(referrerId, planCode)
            analytics.logEvent(
                Events.REFERRAL_ACCEPTED,
                mapOf("referrer" to referrerId, "plan" to planCode)
            )
        }
    }

    fun consumeShareForFill(token: String, onReady: (uri: String) -> Unit) {
        viewModelScope.launch {
            val template = shareForFillService.resolve(token) ?: return@launch
            val tempPdf = withContext(Dispatchers.IO) {
                // We don't have the original PDF bytes (privacy: only the
                // template structure was published), so synthesize a blank
                // single-page PDF the size of the first detected page so
                // the local pipeline can persist + render it.
                val page = template.pages.firstOrNull()
                val width = 612
                val height = 792
                val pdfDoc = android.graphics.pdf.PdfDocument()
                val info = android.graphics.pdf.PdfDocument.PageInfo
                    .Builder(width, height, 1).create()
                val pageHandle = pdfDoc.startPage(info)
                pageHandle.canvas.drawColor(android.graphics.Color.WHITE)
                pdfDoc.finishPage(pageHandle)
                val out = File(context.cacheDir, "share-fill-${UUID.randomUUID()}.pdf")
                pdfDoc.writeTo(out.outputStream())
                pdfDoc.close()
                out
            }
            val bytes = tempPdf.readBytes()
            val newId = formRepository.saveForm(template, bytes, existingId = null)
            analytics.logEvent(Events.FILL_RECEIVED_FROM_LINK, mapOf("token" to token))
            onReady(File(context.cacheDir, "share-fill-${newId}.pdf").toURI().toString())
        }
    }
}
