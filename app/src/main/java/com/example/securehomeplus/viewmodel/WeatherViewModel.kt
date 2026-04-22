package com.example.securehomeplus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securehomeplus.data.api.ForecastResponse
import com.example.securehomeplus.data.api.WeatherResponse
import com.example.securehomeplus.data.repository.WeatherRepository
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()

    private val _weather = MutableLiveData<WeatherResponse?>()
    val weather: LiveData<WeatherResponse?> = _weather

    private val _forecast = MutableLiveData<ForecastResponse?>()
    val forecast: LiveData<ForecastResponse?> = _forecast

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            val result = repository.getCurrentWeather(lat, lon)
            result.onSuccess { weatherData ->
                _weather.value = weatherData
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to fetch weather"
            }
            
            _loading.value = false
        }
    }

    fun fetchForecast(lat: Double, lon: Double) {
        viewModelScope.launch {
            val result = repository.getForecast(lat, lon)
            result.onSuccess { forecastData ->
                _forecast.value = forecastData
            }.onFailure { exception ->
                // Silently fail for forecast, main weather is more important
            }
        }
    }
}
