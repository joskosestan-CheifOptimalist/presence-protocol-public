package com.presenceprotocol.data.ble

import android.content.Context
import android.util.Log
import java.io.File

class FileEncounterStore(
    context: Context
) : EncounterStore {

    private val file = File(context.filesDir, "encounters_queue.jsonl")

    override fun append(ticket: EncounterTicket): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.appendText(ticket.toJson() + "\n")
            Log.d(TAG, "ENCOUNTER_STORE_APPEND ok=true encounterId=${ticket.encounterId} path=${file.absolutePath}")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "ENCOUNTER_STORE_APPEND ok=false encounterId=${ticket.encounterId} err=${t.message}", t)
            false
        }
    }

    override fun count(): Int {
        return try {
            if (!file.exists()) return 0
            file.useLines { lines -> lines.count { it.isNotBlank() } }
        } catch (t: Throwable) {
            Log.e(TAG, "ENCOUNTER_STORE_COUNT err=${t.message}", t)
            0
        }
    }

    companion object {
        private const val TAG = "EncounterStore"
    }
}
