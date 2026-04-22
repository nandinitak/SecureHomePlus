package com.example.securehomeplus.ui.dashboard

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.securehomeplus.R
import com.example.securehomeplus.databinding.ActivityDashboardBinding
import com.example.securehomeplus.ui.survey.SurveyActivity
import com.example.securehomeplus.ui.login.LoginActivity
import com.example.securehomeplus.ui.result.ViewReportsActivity
import com.example.securehomeplus.utils.PreferencesManager
import com.example.securehomeplus.view.account.AccountActivity
import com.example.securehomeplus.view.map.MapActivity
import com.example.securehomeplus.view.settings.SettingsActivity
import com.example.securehomeplus.view.weather.WeatherActivity
import com.example.securehomeplus.view.family.FamilyMembersActivity
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.securehomeplus.viewmodel.FamilyViewModel
import com.example.securehomeplus.viewmodel.ResultViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.securehomeplus.utils.EmergencyAlertHelper
import com.example.securehomeplus.utils.LocationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.LayoutInflater

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var adapter: DashboardAdapter
    private lateinit var prefs: PreferencesManager
    private val familyViewModel: FamilyViewModel by viewModels()
    private val resultViewModel: ResultViewModel by viewModels()

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sendEmergencyAlert()
        } else {
            Toast.makeText(this, "SMS permission required for emergency alerts", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            checkSmsPermissionAndSend()
        } else {
            checkSmsPermissionAndSend() // Send without location
        }
    }

    // Daily Reminder (kept for background notification)
    private fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, com.example.securehomeplus.service.ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + 60 * 1000L // 1 min for testing
        val interval = android.app.AlarmManager.INTERVAL_DAY
        alarmManager.setInexactRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            triggerTime,
            interval,
            pendingIntent
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        // Disable title to prevent overlap with modern header
        supportActionBar?.setDisplayShowTitleEnabled(false)

        prefs = PreferencesManager(this)
        scheduleDailyReminder(this)

        // Dynamic welcome text
        val userName = prefs.getUserName() ?: prefs.getUserEmail()?.substringBefore("@") ?: "User"
        binding.tvWelcome.text = "Welcome, $userName 👋"

        setupRecycler()
        setupFab()
        setupQuickActions()

        // Menu click listener
        binding.toolbar.setOnMenuItemClickListener { item ->
            onOptionsItemSelected(item)
        }

        // Footer quote
        binding.tvQuote.text = "“Your home’s safety is our priority 🔒”"
        binding.tvCopyright.text = "© 2025 SecureHome+ | Crafted by Nandini 💚"
    }

    private fun setupRecycler() {
        val features = listOf(
            DashboardFeature(1, "Start Safety Evaluation", "Quick home check", R.drawable.ic_survey, FeatureAction.START_SURVEY),
            DashboardFeature(2, "Weather Forecast", "Real-time weather", R.drawable.ic_weather_sunny, FeatureAction.VIEW_WEATHER),
            DashboardFeature(3, "Family Members", "Emergency contacts", R.drawable.ic_family, FeatureAction.MANAGE_FAMILY),
            DashboardFeature(4, "View Nearby Points", "Police & Fire", R.drawable.ic_map, FeatureAction.VIEW_MAP),
            DashboardFeature(5, "Security Shop", "Best Appliances", R.drawable.ic_shield, FeatureAction.SECURITY_SHOP),
            DashboardFeature(6, "Safety Resources", "Home Hazards", R.drawable.ic_shield, FeatureAction.SAFETY_RESOURCES),
            DashboardFeature(7, "Emergency Helplines", "National Services", R.drawable.ic_phone, FeatureAction.EMERGENCY_HELPLINES),
            DashboardFeature(8, "View Reports", "Saved checks", R.drawable.ic_reports, FeatureAction.VIEW_REPORTS),
            DashboardFeature(9, "Settings", "Preferences", R.drawable.ic_settings, FeatureAction.OPEN_SETTINGS),
            DashboardFeature(10, "My Account", "Profile", R.drawable.ic_person, FeatureAction.OPEN_ACCOUNT),
            DashboardFeature(11, "Logout", "Sign out", R.drawable.ic_logout, FeatureAction.LOGOUT)
        )

        adapter = DashboardAdapter(features) { feature -> handleFeatureClick(feature) }

        binding.rvFeatures.layoutManager = GridLayoutManager(this, 2)
        binding.rvFeatures.adapter = adapter

        val spacing = (resources.displayMetrics.density * 8).toInt()
        binding.rvFeatures.addItemDecoration(SpaceItemDecoration(spacing))
    }

    private fun setupFab() {
        binding.fabHealthGini.setOnClickListener {
            startActivity(Intent(this, com.example.securehomeplus.ui.ai.HealthGiniActivity::class.java))
        }
    }

    private fun setupQuickActions() {
        // Emergency Alert Button
        binding.cardEmergency.setOnClickListener {
            showEmergencyConfirmation()
        }

        // Quick Survey Button
        binding.cardQuickSurvey.setOnClickListener {
            startActivity(Intent(this, SurveyActivity::class.java))
        }

        // Bell Icon/Notifications - Show latest report
        binding.cardNotifications.setOnClickListener {
            val email = prefs.getUserEmail() ?: return@setOnClickListener
            resultViewModel.getReports(email) { reports ->
                runOnUiThread {
                    if (reports.isNotEmpty()) {
                        val latest = reports.sortedByDescending { it.id }.first()
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Recent Safety Report")
                            .setMessage("Latest Score: ${latest.score}%\nDate: ${latest.summary.substringAfter("Date:").substringBefore(" |")}\n\nWould you like to view all reports?")
                            .setPositiveButton("View All") { _, _ -> 
                                startActivity(Intent(this, ViewReportsActivity::class.java)) 
                            }
                            .setNegativeButton("Close", null)
                            .show()
                    } else {
                        Toast.makeText(this, "No recent reports found", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ViewReportsActivity::class.java))
                    }
                }
            }
        }

        // Account Profile icon
        binding.cardProfile.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
    }

    private fun showEmergencyConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🚨 Send Emergency Alert")
            .setMessage("This will send an emergency SMS to all your family members with your current location. Continue?")
            .setPositiveButton("Send Alert") { _, _ ->
                checkLocationPermissionAndSend()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkLocationPermissionAndSend() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkSmsPermissionAndSend()
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun checkSmsPermissionAndSend() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            sendEmergencyAlert()
        } else {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun sendEmergencyAlert() {
        val userEmail = prefs.getUserEmail() ?: return
        familyViewModel.loadFamilyMembers(userEmail)
        
        // Use a slight delay to ensure members are loaded or use observer
        // Better yet, just observe or wait. For simplicity and since helper handles list:
        familyViewModel.familyMembers.observe(this) { members ->
            if (members.isNullOrEmpty()) {
                Toast.makeText(this, "No family members added to send alerts!", Toast.LENGTH_LONG).show()
                return@observe
            }
            
            val userName = userEmail.substringBefore("@")
            CoroutineScope(Dispatchers.Main).launch {
                val location = withContext(Dispatchers.IO) {
                    try { LocationUtils.getLastLocation(this@DashboardActivity) } catch (e: Exception) { null }
                }
                
                val result = EmergencyAlertHelper.sendEmergencyAlert(this@DashboardActivity, members, userName, location)
                result.onSuccess { msg ->
                    Toast.makeText(this@DashboardActivity, msg, Toast.LENGTH_LONG).show()
                    EmergencyAlertHelper.sendEmergencyNotification(this@DashboardActivity, members, userName)
                }.onFailure { err ->
                    Toast.makeText(this@DashboardActivity, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
            // Remove observer after use
            familyViewModel.familyMembers.removeObservers(this)
        }
    }

    private fun handleFeatureClick(feature: DashboardFeature) {
        when (feature.actionType) {
            FeatureAction.START_SURVEY -> startActivity(Intent(this, SurveyActivity::class.java))
            FeatureAction.VIEW_WEATHER -> startActivity(Intent(this, WeatherActivity::class.java))
            FeatureAction.MANAGE_FAMILY -> startActivity(Intent(this, FamilyMembersActivity::class.java))
            FeatureAction.VIEW_MAP -> startActivity(Intent(this, MapActivity::class.java))
            FeatureAction.VIEW_REPORTS -> startActivity(Intent(this, ViewReportsActivity::class.java))
            FeatureAction.OPEN_SETTINGS -> startActivity(Intent(this, SettingsActivity::class.java))
            FeatureAction.OPEN_ACCOUNT -> startActivity(Intent(this, AccountActivity::class.java))
            FeatureAction.LOGOUT -> performLogout()
            FeatureAction.EMERGENCY_ALERT -> startActivity(Intent(this, FamilyMembersActivity::class.java))
            FeatureAction.SECURITY_SHOP -> {
                val intent = Intent(this, com.example.securehomeplus.ui.webview.WebViewActivity::class.java)
                intent.putExtra("EXTRA_TITLE", "Security Shop")
                intent.putExtra("EXTRA_URL", "https://homemate.co.in/product-category/security/?srsltid=AfmBOop-PkaQoaurkmDzDGjPsDBYC3jXdkT8BAwdNi9T-7GksWn8bViZ")
                startActivity(intent)
            }
            FeatureAction.SAFETY_RESOURCES -> {
                val intent = Intent(this, com.example.securehomeplus.ui.webview.WebViewActivity::class.java)
                intent.putExtra("EXTRA_TITLE", "Safety Resources")
                intent.putExtra("EXTRA_URL", "https://www.safehome.org/resources/home-hazards/")
                startActivity(intent)
            }
            FeatureAction.EMERGENCY_HELPLINES -> {
                startActivity(Intent(this, com.example.securehomeplus.view.helplines.HelplinesActivity::class.java))
            }
        }
    }

    private fun performLogout() {
        prefs.clearSession()
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
        val i = Intent(this, LoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_account -> {
                startActivity(Intent(this, AccountActivity::class.java))
                true
            }
            R.id.menu_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
