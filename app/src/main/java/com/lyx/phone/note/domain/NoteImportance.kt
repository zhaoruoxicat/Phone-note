package com.lyx.phone.note.domain

enum class NoteImportance(val displayName: String) {
    NORMAL("普通"),
    IMPORTANT("重要"),
    CAUTION("谨慎接听");

    companion object {
        fun fromName(name: String): NoteImportance = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}
