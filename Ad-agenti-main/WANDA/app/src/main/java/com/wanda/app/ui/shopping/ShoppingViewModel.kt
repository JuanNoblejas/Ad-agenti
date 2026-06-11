package com.wanda.app.ui.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wanda.app.WandaApplication
import com.wanda.app.data.database.entity.ShoppingItem
import com.wanda.app.data.repository.ShoppingRepository
import kotlinx.coroutines.launch

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShoppingRepository =
        (application as WandaApplication).shoppingRepository

    val allItems: LiveData<List<ShoppingItem>> = repository.allItems.asLiveData()

    fun addItem(name: String) {
        viewModelScope.launch {
            repository.insert(ShoppingItem(name = name))
        }
    }

    fun toggleChecked(item: ShoppingItem) {
        viewModelScope.launch {
            repository.toggleChecked(item)
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun deleteCheckedItems() {
        viewModelScope.launch {
            repository.deleteCheckedItems()
        }
    }
}
