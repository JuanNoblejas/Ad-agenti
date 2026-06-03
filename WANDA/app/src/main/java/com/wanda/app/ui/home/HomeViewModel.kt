package com.wanda.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.wanda.app.WandaApplication
import com.wanda.app.data.database.entity.GpsRecord
import com.wanda.app.data.repository.GpsRepository
import com.wanda.app.utils.PreferencesManager
import kotlinx.coroutines.flow.map

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as WandaApplication
    private val gpsRepository: GpsRepository = app.gpsRepository
    private val preferencesManager: PreferencesManager = app.preferencesManager

    val allRecords: LiveData<List<GpsRecord>> = gpsRepository.allRecords.asLiveData()

    val latestRecord: LiveData<GpsRecord?> = gpsRepository.allRecords
        .map { records -> records.firstOrNull() }
        .asLiveData()

    fun getPreferencesManager(): PreferencesManager = preferencesManager
}
