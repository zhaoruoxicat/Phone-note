package com.lyx.phone.note.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import com.lyx.phone.note.LocalCallNoteApp
import com.lyx.phone.note.data.db.NumberNoteEntity
import com.lyx.phone.note.overlay.IncomingOverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocalCallScreeningService : CallScreeningService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        if (number.isNullOrBlank()) {
            IncomingOverlayController.dismiss(applicationContext)
            respondToCall(callDetails, allowResponse())
            return
        }

        val note = findNoteQuickly(number)
        val activeNote = note?.takeUnless { it.isArchived }

        if (activeNote?.rejectIncomingCall == true) {
            IncomingOverlayController.dismiss(applicationContext)
            respondToCall(callDetails, rejectResponse())
            return
        }

        respondToCall(callDetails, allowResponse())

        if (activeNote?.enableIncomingPopup == true) {
            serviceScope.launch {
                withContext(Dispatchers.Main) {
                    IncomingOverlayController.show(applicationContext, activeNote)
                }
            }
        } else {
            IncomingOverlayController.dismiss(applicationContext)
        }
    }

    private fun findNoteQuickly(number: String): NumberNoteEntity? =
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(1200) {
                runCatching {
                    (application as LocalCallNoteApp).repository.findNoteForIncomingCall(number)
                }.getOrNull()
            }
        }

    private fun allowResponse(): CallResponse =
        CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

    private fun rejectResponse(): CallResponse =
        CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSilenceCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
}
