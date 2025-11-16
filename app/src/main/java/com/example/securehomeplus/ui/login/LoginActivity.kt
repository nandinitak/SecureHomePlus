package com.example.securehomeplus.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.securehomeplus.data.database.AppDatabase
import com.example.securehomeplus.data.repository.UserRepository
import com.example.securehomeplus.databinding.ActivityLoginBinding
import com.example.securehomeplus.ui.dashboard.DashboardActivity
import com.example.securehomeplus.ui.register.RegisterActivity
import com.example.securehomeplus.utils.PreferencesManager
import com.example.securehomeplus.utils.ValidationUtils
import com.example.securehomeplus.viewmodel.AuthViewModel
import com.example.securehomeplus.viewmodel.AuthViewModelFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel
    private lateinit var prefs: PreferencesManager

    private var biometricPrompt: BiometricPrompt? = null
    private var promptInfo: BiometricPrompt.PromptInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        // Auto-login using fingerprint (if enabled)
        if (!prefs.isLoggedIn() &&
            prefs.isBiometricEnabled() &&
            prefs.getBiometricUser() != null
        ) {
            setupBiometricPrompt()
            biometricPrompt?.authenticate(promptInfo!!)
        }

        //  If normal session exists
        if (prefs.isLoggedIn()) {
            goToDashboard()
            return
        }

        setupViewModel()
        setupLoginButton()
        setupRegisterButton()
        setupBiometricSwitch()
        setupFingerprintIcon()
    }

    // ---------------- VIEWMODEL ----------------
    private fun setupViewModel() {
        val dao = AppDatabase.getDatabase(this).userDao()
        val repo = UserRepository(dao)
        val factory = AuthViewModelFactory(repo)

        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        viewModel.loginResult.observe(this) { user ->
            if (user != null) {

                prefs.saveLogin(user.email)

                //  If user enabled biometric → bind this email
                if (prefs.isBiometricEnabled()) {
                    prefs.saveBiometricUser(user.email)
                }

                Toast.makeText(this, "Welcome ${user.name}", Toast.LENGTH_SHORT).show()
                goToDashboard()

            } else {
                Toast.makeText(this, "Invalid credentials!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    // -------------- LOGIN BUTTON --------------
    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            when {
                !ValidationUtils.isValidEmail(email) ->
                    binding.etEmail.error = "Invalid email"

                password.isEmpty() ->
                    binding.etPassword.error = "Enter password"

                else -> viewModel.loginUser(email, password)
            }
        }
    }

    // -------------- REGISTER BUTTON --------------
    private fun setupRegisterButton() {
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // -------------- BIOMETRIC ENABLE SWITCH --------------
    private fun setupBiometricSwitch() {

        binding.switchBiometric.isChecked = prefs.isBiometricEnabled()

        binding.switchBiometric.setOnCheckedChangeListener { _, enabled ->

            prefs.setBiometricEnabled(enabled)
            Toast.makeText(this, "Biometric login: $enabled", Toast.LENGTH_SHORT).show()

            if (enabled) {
                setupBiometricPrompt()
                // Bind logged in user
                prefs.getUserEmail()?.let { prefs.saveBiometricUser(it) }
            }
        }
    }

    // -------------- FINGERPRINT ICON CLICK --------------
    private fun setupFingerprintIcon() {
        binding.ivFingerprint.setOnClickListener {

            if (!prefs.isBiometricEnabled()) {
                Toast.makeText(this, "Enable biometric login first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (promptInfo == null) {
                Toast.makeText(this, "Biometric not supported.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            biometricPrompt?.authenticate(promptInfo!!)
        }
    }

    // -------------- BIOMETRIC PROMPT SETUP --------------
    private fun setupBiometricPrompt() {

        val manager = BiometricManager.from(this)

        val canAuth = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Fingerprint not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Toast.makeText(applicationContext, "Fingerprint Verified!", Toast.LENGTH_SHORT).show()

                    val email = prefs.getBiometricUser()
                    if (email != null) {
                        prefs.saveLogin(email)
                    }

                    goToDashboard()
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(applicationContext, "Try again...", Toast.LENGTH_SHORT).show()
                }
            }
        )

        //  FINAL FIX → NO NEGATIVE BUTTON (mandatory)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Login")
            .setSubtitle("Use fingerprint to continue")
            .setNegativeButtonText("Cancel")
            .build()
    }
}
