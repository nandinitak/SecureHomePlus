package com.example.securehomeplus.view.family

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securehomeplus.R
import com.example.securehomeplus.data.database.entities.FamilyMember
import com.example.securehomeplus.databinding.ActivityFamilyMembersBinding
import com.example.securehomeplus.databinding.DialogAddFamilyMemberBinding
import com.example.securehomeplus.utils.EmergencyAlertHelper
import com.example.securehomeplus.utils.LocationUtils
import com.example.securehomeplus.utils.PreferencesManager
import com.example.securehomeplus.viewmodel.FamilyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FamilyMembersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFamilyMembersBinding
    private val viewModel: FamilyViewModel by viewModels()
    private lateinit var adapter: FamilyMembersAdapter
    private lateinit var prefs: PreferencesManager

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        loadFamilyMembers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = FamilyMembersAdapter(
            onDeleteClick = { member ->
                showDeleteConfirmation(member)
            }
        )
        binding.rvFamilyMembers.layoutManager = LinearLayoutManager(this)
        binding.rvFamilyMembers.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.familyMembers.observe(this) { members ->
            if (members.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvFamilyMembers.visibility = View.GONE
                binding.btnEmergencyAlert.isEnabled = false
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvFamilyMembers.visibility = View.VISIBLE
                binding.btnEmergencyAlert.isEnabled = true
                adapter.submitList(members)
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddMember.setOnClickListener {
            showAddMemberDialog()
        }

        binding.btnEmergencyAlert.setOnClickListener {
            showEmergencyConfirmation()
        }
    }

    private fun loadFamilyMembers() {
        val userEmail = prefs.getUserEmail() ?: return
        viewModel.loadFamilyMembers(userEmail)
    }

    private fun showAddMemberDialog() {
        val dialogBinding = DialogAddFamilyMemberBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        // Setup relationship dropdown
        val relationships = arrayOf("Father", "Mother", "Spouse", "Child", "Sibling", "Friend", "Other")
        val relationshipAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, relationships)
        dialogBinding.actvRelationship.setAdapter(relationshipAdapter)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnAdd.setOnClickListener {
            val name = dialogBinding.etName.text.toString().trim()
            val phone = dialogBinding.etPhone.text.toString().trim()
            val relationship = dialogBinding.actvRelationship.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || relationship.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.length < 10) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userEmail = prefs.getUserEmail() ?: return@setOnClickListener
            val member = FamilyMember(
                userEmail = userEmail,
                name = name,
                phoneNumber = phone,
                relationship = relationship
            )

            viewModel.addFamilyMember(member) { success ->
                if (success) {
                    Toast.makeText(this, "Family member added ✅", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Failed to add member", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(member: FamilyMember) {
        AlertDialog.Builder(this)
            .setTitle("Delete Family Member")
            .setMessage("Are you sure you want to remove ${member.name}?")
            .setPositiveButton("Delete") { _, _ ->
                val userEmail = prefs.getUserEmail() ?: return@setPositiveButton
                viewModel.deleteFamilyMember(member, userEmail)
                Toast.makeText(this, "${member.name} removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEmergencyConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("🚨 Send Emergency Alert")
            .setMessage("This will send an emergency SMS to all your family members with your location. Continue?")
            .setPositiveButton("Send Alert") { _, _ ->
                checkLocationPermissionAndSend()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkLocationPermissionAndSend() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkSmsPermissionAndSend()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun checkSmsPermissionAndSend() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sendEmergencyAlert()
        } else {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun sendEmergencyAlert() {
        val members = viewModel.familyMembers.value ?: emptyList()
        if (members.isEmpty()) {
            Toast.makeText(this, "No family members to alert", Toast.LENGTH_SHORT).show()
            return
        }

        val userName = prefs.getUserEmail()?.substringBefore("@") ?: "User"

        CoroutineScope(Dispatchers.Main).launch {
            val location = withContext(Dispatchers.IO) {
                try {
                    LocationUtils.getLastLocation(this@FamilyMembersActivity)
                } catch (e: Exception) {
                    null
                }
            }

            val result = EmergencyAlertHelper.sendEmergencyAlert(
                this@FamilyMembersActivity,
                members,
                userName,
                location
            )

            result.onSuccess { message ->
                Toast.makeText(this@FamilyMembersActivity, message, Toast.LENGTH_LONG).show()
                EmergencyAlertHelper.sendEmergencyNotification(
                    this@FamilyMembersActivity,
                    members,
                    userName
                )
            }.onFailure { error ->
                Toast.makeText(
                    this@FamilyMembersActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
