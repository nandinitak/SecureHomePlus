package com.example.securehomeplus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.securehomeplus.data.database.AppDatabase
import com.example.securehomeplus.data.database.entities.FamilyMember
import com.example.securehomeplus.data.repository.FamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FamilyRepository
    
    private val _familyMembers = MutableLiveData<List<FamilyMember>>()
    val familyMembers: LiveData<List<FamilyMember>> = _familyMembers

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        val dao = AppDatabase.getDatabase(application).familyMemberDao()
        repository = FamilyRepository(dao)
    }

    fun loadFamilyMembers(userEmail: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val members = withContext(Dispatchers.IO) {
                    repository.getFamilyMembers(userEmail)
                }
                _familyMembers.value = members
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun addFamilyMember(member: FamilyMember, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.addFamilyMember(member)
                }
                loadFamilyMembers(member.userEmail)
                onComplete(true)
            } catch (e: Exception) {
                _error.value = e.message
                onComplete(false)
            }
        }
    }

    fun deleteFamilyMember(member: FamilyMember, userEmail: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteFamilyMember(member)
                }
                loadFamilyMembers(userEmail)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
