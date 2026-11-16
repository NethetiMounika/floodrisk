package com.example.floodrisk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.floodrisk.auth.FirebaseAuthHelper
import com.example.floodrisk.model.FloodForecastDay
import com.example.floodrisk.model.MonitoredPlace
import com.example.floodrisk.model.UserProfile
import com.example.floodrisk.notification.NotificationHelper
import com.example.floodrisk.repository.FloodRepository
import com.example.floodrisk.utils.RiskCalculator
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FloodRepository(application)
    private val authHelper = FirebaseAuthHelper(application)
    private val notificationHelper = NotificationHelper(application)

    private val _currentUser = MutableLiveData<UserProfile?>()
    val currentUser: LiveData<UserProfile?> = _currentUser

    private val _selectedPlace = MutableLiveData<MonitoredPlace?>()
    val selectedPlace: LiveData<MonitoredPlace?> = _selectedPlace

    private val _allPlaces = MutableLiveData<List<MonitoredPlace>>()
    val allPlaces: LiveData<List<MonitoredPlace>> = _allPlaces

    private val _forecastDays = MutableLiveData<List<FloodForecastDay>>()
    val forecastDays: LiveData<List<FloodForecastDay>> = _forecastDays

    private val _todayStatus = MutableLiveData<FloodForecastDay?>()
    val todayStatus: LiveData<FloodForecastDay?> = _todayStatus

    private val _peakDay = MutableLiveData<FloodForecastDay?>()
    val peakDay: LiveData<FloodForecastDay?> = _peakDay

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _locationInfo = MutableLiveData<String>()
    val locationInfo: LiveData<String> = _locationInfo

    init {
        loadUserSession()
        observePlaces()
    }

    fun loadUserSession() {
        _currentUser.value = authHelper.getCurrentUser()
    }

    private fun observePlaces() {
        viewModelScope.launch {
            repository.allMonitoredPlaces.collect { places ->
                _allPlaces.value = places
                if (_selectedPlace.value == null && places.isNotEmpty()) {
                    val defaultPlace = places.find { it.isDefault } ?: places.first()
                    _selectedPlace.value = defaultPlace
                    fetchForecastForPlace(defaultPlace)
                }
            }
        }
    }

    fun detectCurrentLocationAndFetch() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _locationInfo.value = "Detecting current location..."

            if (!repository.hasLocationPermissions()) {
                _isLoading.value = false
                _errorMessage.value = "Location permission not granted. Please enable location permissions."
                _locationInfo.value = "Permission Denied"
                return@launch
            }

            val location = repository.getCurrentLocation()
            if (location != null) {
                val currentPlace = MonitoredPlace(
                    name = "Current Location",
                    latitude = location.latitude,
                    longitude = location.longitude,
                    isDefault = false
                )
                _locationInfo.value = String.format(Locale.US, "Lat: %.4f, Lon: %.4f", location.latitude, location.longitude)
                _selectedPlace.value = currentPlace
                fetchForecastForPlace(currentPlace)
            } else {
                _isLoading.value = false
                _locationInfo.value = "Location Unavailable"
                _errorMessage.value = "Could not detect location. Make sure GPS is enabled and try again."
            }
        }
    }

    fun selectPlace(place: MonitoredPlace) {
        _selectedPlace.value = place
        _locationInfo.value = String.format(Locale.US, "%.4f, %.4f", place.latitude, place.longitude)
        fetchForecastForPlace(place)
    }

    fun refresh() {
        val current = _selectedPlace.value
        if (current != null) {
            fetchForecastForPlace(current)
        } else {
            detectCurrentLocationAndFetch()
        }
    }

    private fun fetchForecastForPlace(place: MonitoredPlace) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.fetchFloodForecast(place.latitude, place.longitude)
            _isLoading.value = false

            if (result.isSuccess) {
                val days = result.getOrDefault(emptyList())
                _forecastDays.value = days
                _todayStatus.value = days.firstOrNull()
                _peakDay.value = RiskCalculator.findPeakDay(days)

                // Check 7-day alerts
                val alerts = RiskCalculator.getUpcomingAlerts(days, 7)
                for (alertDay in alerts) {
                    notificationHelper.notifyFloodAlert(place.name, alertDay)
                }
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Failed to fetch flood data"
                _errorMessage.value = error
                _forecastDays.value = emptyList()
                _todayStatus.value = null
                _peakDay.value = null
            }
        }
    }

    fun logout() {
        authHelper.signOut()
        _currentUser.value = null
    }
}
