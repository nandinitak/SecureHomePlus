package com.example.securehomeplus.view.weather

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.securehomeplus.R
import com.example.securehomeplus.databinding.ActivityWeatherEnhancedBinding
import com.example.securehomeplus.utils.LocationUtils
import com.example.securehomeplus.viewmodel.WeatherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherEnhancedBinding
    private val viewModel: WeatherViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchWeatherData()
        } else {
            Toast.makeText(this, "Location permission required for weather", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherEnhancedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupObservers()
        checkLocationAndFetchWeather()

        binding.btnRefresh.setOnClickListener {
            fetchWeatherData()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupObservers() {
        viewModel.weather.observe(this) { weather ->
            weather?.let {
                // Animate content visibility
                binding.weatherContent.visibility = View.VISIBLE
                binding.errorLayout.visibility = View.GONE
                animateContentIn()

                // Update main weather card
                binding.tvLocation.text = it.name
                binding.tvTemperature.text = "${it.main.temp.toInt()}°C"
                binding.tvDescription.text = it.weather.firstOrNull()?.description?.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                } ?: ""
                binding.tvFeelsLike.text = "Feels like ${it.main.feels_like.toInt()}°C"

                // Update weather card view with canvas animation
                binding.weatherCardView.setWeatherData(
                    it.main.temp.toFloat(),
                    it.main.humidity.toFloat()
                )

                // Update detail cards with animations
                animateValue(binding.tvHumidityValue, 0, it.main.humidity, "%")
                animateValue(binding.tvWindValue, 0f, it.wind.speed.toFloat(), " m/s")
                animateValue(binding.tvPressureValue, 0, it.main.pressure, " hPa")
                animateValue(binding.tvVisibilityValue, 0f, (it.visibility / 1000f), " km")

                // Temperature range
                binding.tvTempMin.text = "${it.main.temp_min.toInt()}°C"
                binding.tvTempMax.text = "${it.main.temp_max.toInt()}°C"

                // Sun times
                val sunrise = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.sys.sunrise * 1000))
                val sunset = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.sys.sunset * 1000))
                binding.tvSunrise.text = sunrise
                binding.tvSunset.text = sunset

                // Weather icon based on condition
                val iconRes = when {
                    it.weather.firstOrNull()?.main?.contains("Rain", true) == true -> R.drawable.ic_weather_rain
                    it.weather.firstOrNull()?.main?.contains("Cloud", true) == true -> R.drawable.ic_weather_cloudy
                    it.weather.firstOrNull()?.main?.contains("Clear", true) == true -> R.drawable.ic_weather_sunny
                    it.weather.firstOrNull()?.main?.contains("Snow", true) == true -> R.drawable.ic_weather_cloudy
                    else -> R.drawable.ic_weather_sunny
                }
                binding.ivWeatherIcon.setImageResource(iconRes)
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRefresh.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.errorLayout.visibility = View.VISIBLE
                binding.tvError.text = it
                binding.weatherContent.visibility = View.GONE
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun animateContentIn() {
        // Animate cards with stagger effect
        val cards = listOf(
            binding.cardHumidity,
            binding.cardWind,
            binding.cardPressure,
            binding.cardVisibility,
            binding.cardSunTimes,
            binding.cardTempRange
        )

        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 50f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay((index * 100).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun animateValue(textView: android.widget.TextView, start: Number, end: Number, suffix: String) {
        val animator = when (end) {
            is Int -> ObjectAnimator.ofInt(start as Int, end)
            is Float -> ObjectAnimator.ofFloat(start as Float, end)
            else -> return
        }

        animator.duration = 1500
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue
            textView.text = when (value) {
                is Int -> "$value$suffix"
                is Float -> String.format("%.1f%s", value, suffix)
                else -> "$value$suffix"
            }
        }
        animator.start()
    }

    private fun checkLocationAndFetchWeather() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchWeatherData()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchWeatherData() {
        CoroutineScope(Dispatchers.Main).launch {
            val location = withContext(Dispatchers.IO) {
                LocationUtils.getLastLocation(this@WeatherActivity)
            }

            if (location != null) {
                viewModel.fetchWeather(location.latitude, location.longitude)
            } else {
                // Default to Delhi coordinates if location not available
                viewModel.fetchWeather(28.6139, 77.2090)
                Toast.makeText(
                    this@WeatherActivity,
                    "Using default location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
