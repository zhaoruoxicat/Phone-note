package com.lyx.phone.note.domain

object CategoryDisplay {
    fun labelOf(value: String): String =
        NoteCategory.entries.firstOrNull { it.name == value }?.displayName ?: value

    fun storageValueOf(label: String): String =
        NoteCategory.entries.firstOrNull { it.displayName == label }?.name ?: label
}
