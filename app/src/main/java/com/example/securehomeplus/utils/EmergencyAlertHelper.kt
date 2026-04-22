package com.example.securehomeplus.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import com.example.securehomeplus.data.database.entities.FamilyMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmergencyAlertHelper {

    /**
     * Send emergency SMS to all family members
     */
    suspend fun sendEmergencyAlert(
        context: Context,
        familyMembers: List<FamilyMember>,
        userName: String,
        location: Location?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext Result.failure(Exception("SMS permission not granted"))
            }

            if (familyMembers.isEmpty()) {
                return@withContext Result.failure(Exception("No family members added"))
            }

            val locationText = if (location != null) {
                "Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                "Location: Not available"
            }

            val message = """
                🚨 EMERGENCY ALERT 🚨
                $userName needs help!
                $locationText
                Please respond immediately.
                - SecureHome+ Alert System
            """.trimIndent()

            val smsManager = SmsManager.getDefault()
            var successCount = 0

            familyMembers.forEach { member ->
                try {
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(
                        member.phoneNumber,
                        null,
                        parts,
                        null,
                        null
                    )
                    successCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (successCount > 0) {
                Result.success("Emergency alert sent to $successCount family member(s)")
            } else {
                Result.failure(Exception("Failed to send alerts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send emergency notification to all family members (in-app)
     */
    fun sendEmergencyNotification(
        context: Context,
        familyMembers: List<FamilyMember>,
        userName: String
    ) {
        val title = "🚨 Emergency Alert"
        val message = "$userName has triggered an emergency alert! ${familyMembers.size} family member(s) notified."
        NotificationUtils.showNotification(context, title, message)
    }
}
