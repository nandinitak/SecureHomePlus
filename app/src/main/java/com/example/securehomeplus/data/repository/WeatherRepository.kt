package com.example.securehomeplus.data.repository

import com.example.securehomeplus.data.api.ForecastResponse
import com.example.securehomeplus.data.api.RetrofitClient
import com.example.securehomeplus.data.api.WeatherResponse

class WeatherRepository {
    private val api = RetrofitClient.weatherApi
    
    // OpenWeatherMap API Key
    private val API_KEY = "7a20cc3a76b21cf8b3bfabdea1f383f5"

    suspend fun getCurrentWeather(lat: Double, lon: Double): Result<WeatherResponse> {
        return try {
            val response = api.getCurrentWeather(lat, lon, API_KEY)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Weather API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getForecast(lat: Double, lon: Double): Result<ForecastResponse> {
        return try {
            val response = api.getForecast(lat, lon, API_KEY)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Forecast API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
