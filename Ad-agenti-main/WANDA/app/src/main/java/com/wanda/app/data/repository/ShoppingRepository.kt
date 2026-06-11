package com.wanda.app.data.repository

import com.wanda.app.data.database.dao.ShoppingDao
import com.wanda.app.data.database.entity.ShoppingItem
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(private val dao: ShoppingDao) {

    val allItems: Flow<List<ShoppingItem>> = dao.getAllItems()

    suspend fun insert(item: ShoppingItem) {
        dao.insert(item)
    }

    suspend fun update(item: ShoppingItem) {
        dao.update(item)
    }

    suspend fun delete(item: ShoppingItem) {
        dao.delete(item)
    }

    suspend fun deleteCheckedItems() {
        dao.deleteCheckedItems()
    }

    suspend fun toggleChecked(item: ShoppingItem) {
        dao.update(item.copy(isChecked = !item.isChecked))
    }
}
