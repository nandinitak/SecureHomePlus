package com.example.securehomeplus.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userEmail: String,  // Owner of this family member
    val name: String,
    val phoneNumber: String,
    val relationship: String,  // e.g., "Father", "Mother", "Spouse", "Child"
    val photoUri: String? = null,
    val isEmergencyContact: Boolean = true
)
