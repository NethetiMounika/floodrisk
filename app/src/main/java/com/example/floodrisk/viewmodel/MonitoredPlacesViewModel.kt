package com.example.floodrisk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.floodrisk.model.MonitoredPlace
import com.example.floodrisk.repository.FloodRepository
import kotlinx.coroutines.launch

class MonitoredPlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FloodRepository(application)

    private val _places = MutableLiveData<List<MonitoredPlace>>()
    val places: LiveData<List<MonitoredPlace>> = _places

    private val _operationStatus = MutableLiveData<String?>()
    val operationStatus: LiveData<String?> = _operationStatus

    init {
        loadPlaces()
    }

    private fun loadPlaces() {
        viewModelScope.launch {
            repository.allMonitoredPlaces.collect { list ->
                _places.value = list
            }
        }
    }

    fun addPlace(name: String, latitude: Double, longitude: Double, isDefault: Boolean) {
        viewModelScope.launch {
            val place = MonitoredPlace(
                name = name.trim(),
                latitude = latitude,
                longitude = longitude,
                isDefault = isDefault
            )
            repository.addMonitoredPlace(place)
            _operationStatus.value = "Place saved successfully."
        }
    }

    fun updatePlace(id: Long, name: String, latitude: Double, longitude: Double, isDefault: Boolean) {
        viewModelScope.launch {
            val place = MonitoredPlace(
                id = id,
                name = name.trim(),
                latitude = latitude,
                longitude = longitude,
                isDefault = isDefault
            )
            repository.updateMonitoredPlace(place)
            if (isDefault) {
                repository.setDefaultPlace(id)
            }
            _operationStatus.value = "Place updated successfully."
        }
    }

    fun deletePlace(place: MonitoredPlace) {
        viewModelScope.launch {
            repository.deleteMonitoredPlace(place)
            _operationStatus.value = "Place deleted."
        }
    }

    fun setDefaultPlace(placeId: Long) {
        viewModelScope.launch {
            repository.setDefaultPlace(placeId)
            _operationStatus.value = "Default place updated."
        }
    }

    fun validateInput(name: String, latStr: String, lonStr: String): String? {
        if (name.isBlank()) return "Place name cannot be empty."
        val lat = latStr.toDoubleOrNull() ?: return "Invalid latitude format."
        val lon = lonStr.toDoubleOrNull() ?: return "Invalid longitude format."
        if (lat < -90.0 || lat > 90.0) return "Latitude must be between -90 and 90."
        if (lon < -180.0 || lon > 180.0) return "Longitude must be between -180 and 180."
        return null
    }

    fun clearStatus() {
        _operationStatus.value = null
    }
}
