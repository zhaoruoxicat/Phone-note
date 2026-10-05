package com.lyx.phone.note.data.repository

import com.lyx.phone.note.data.db.NoteCategoryDao
import com.lyx.phone.note.data.db.NoteCategoryEntity
import kotlinx.coroutines.flow.Flow

class NoteCategoryRepository(
    private val dao: NoteCategoryDao
) {
    val builtInNames = listOf("快递", "门店", "售后", "维修", "中介", "客服", "临时联系人", "其他")

    fun observeCategories(): Flow<List<NoteCategoryEntity>> = dao.observeCategories()

    suspend fun ensureDefaults() {
        if (dao.count() > 0) return
        builtInNames.forEachIndexed { index, name ->
            dao.insert(NoteCategoryEntity(name = name, isBuiltIn = true, sortOrder = index))
        }
    }

    suspend fun addCategory(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        return dao.insert(
            NoteCategoryEntity(
                name = trimmed.take(20),
                isBuiltIn = false,
                sortOrder = 100
            )
        ) > 0
    }

    suspend fun updateCategory(category: NoteCategoryEntity, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        return runCatching {
            dao.update(category.copy(name = trimmed.take(20)))
        }.isSuccess
    }

    suspend fun deleteCategory(id: Long) {
        dao.delete(id)
    }
}
