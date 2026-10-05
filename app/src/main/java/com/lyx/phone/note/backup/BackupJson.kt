package com.lyx.phone.note.backup

import com.lyx.phone.note.data.db.NumberNoteEntity
import com.lyx.phone.note.domain.CategoryDisplay
import org.json.JSONArray
import org.json.JSONObject

object BackupJson {
    fun export(notes: List<NumberNoteEntity>, now: Long = System.currentTimeMillis()): String {
        val jsonNotes = JSONArray()
        notes.forEach { note ->
            jsonNotes.put(
                JSONObject()
                    .put("rawNumber", note.rawNumber)
                    .put("normalizedNumber", note.normalizedNumber)
                    .put("last11Digits", note.last11Digits)
                    .put("displayLabel", note.displayLabel)
                    .put("note", note.note)
                    .put("category", note.category)
                    .put("importance", note.importance)
                    .put("enableIncomingPopup", note.enableIncomingPopup)
                    .put("rejectIncomingCall", note.rejectIncomingCall)
                    .put("createdAt", note.createdAt)
                    .put("updatedAt", note.updatedAt)
            )
        }
        return JSONObject()
            .put("version", 1)
            .put("exportedAt", now)
            .put("app", "LocalCallNote")
            .put("notes", jsonNotes)
            .toString(2)
    }

    fun parse(json: String): List<NumberNoteEntity> {
        val root = JSONObject(json)
        val notes = root.getJSONArray("notes")
        return buildList {
            for (index in 0 until notes.length()) {
                val item = notes.getJSONObject(index)
                add(
                    NumberNoteEntity(
                        rawNumber = item.getString("rawNumber"),
                        normalizedNumber = item.getString("normalizedNumber"),
                        last11Digits = item.optString("last11Digits").takeIf { it.isNotBlank() && it != "null" },
                        displayLabel = item.getString("displayLabel"),
                        note = item.optString("note"),
                        category = CategoryDisplay.storageValueOf(item.optString("category", "其他")),
                        importance = item.optString("importance", "NORMAL"),
                        enableIncomingPopup = item.optBoolean("enableIncomingPopup", true),
                        rejectIncomingCall = item.optBoolean("rejectIncomingCall", false),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }
    }
}
