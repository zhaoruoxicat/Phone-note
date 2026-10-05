package com.lyx.phone.note.domain

enum class NoteCategory(val displayName: String) {
    EXPRESS("快递"),
    STORE("门店"),
    AFTER_SALES("售后"),
    REPAIR("维修"),
    REAL_ESTATE("中介"),
    CUSTOMER_SERVICE("客服"),
    TEMP_CONTACT("临时联系人"),
    OTHER("其他");

    companion object {
        fun fromName(name: String): NoteCategory = entries.firstOrNull { it.name == name } ?: OTHER
    }
}
