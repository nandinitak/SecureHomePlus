package com.example.securehomeplus.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("secure_home_prefs", Context.MODE_PRIVATE)

    fun saveUserLogin(email: String) {
        prefs.edit().putString("user_email", email).apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    //  Added for LoginActivity support
    fun isLoggedIn(): Boolean {
        return getUserEmail() != null
    }

    fun saveLogin(email: String) {
        saveUserLogin(email)

    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("biometric_enabled", false)
    }



    fun saveBiometricUser(email: String) {
        prefs.edit().putString("biometric_user", email).apply()
    }

    fun getBiometricUser(): String? {
        return prefs.getString("biometric_user", null)
    }


}
