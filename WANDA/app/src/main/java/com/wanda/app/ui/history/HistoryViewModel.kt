package com.wanda.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.wanda.app.WandaApplication
import com.wanda.app.data.database.entity.GpsRecord
import com.wanda.app.data.repository.GpsRepository

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GpsRepository =
        (application as WandaApplication).gpsRepository

    val allRecords: LiveData<List<GpsRecord>> = repository.allRecords.asLiveData()
}
