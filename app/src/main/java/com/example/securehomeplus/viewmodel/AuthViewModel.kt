package com.example.securehomeplus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securehomeplus.data.database.entities.User
import com.example.securehomeplus.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData

class AuthViewModel(private val repository: UserRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<User?>()
    val loginResult: LiveData<User?> get() = _loginResult

    private val _registerResult = MutableLiveData<Boolean>()
    val registerResult: LiveData<Boolean> get() = _registerResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.registerUser(User(name = name, email = email, password = password))
            result.onSuccess {
                _registerResult.postValue(true)
            }.onFailure { error ->
                _errorMessage.postValue(error.message)
                _registerResult.postValue(false)
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.loginUser(email, password)
            withContext(Dispatchers.Main) {
                result.onSuccess { user ->
                    _loginResult.value = user
                }.onFailure { error ->
                    _errorMessage.value = null // Reset first
                    _errorMessage.value = error.message
                    _loginResult.value = null
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun logout() {
        repository.logout()
    }
}
