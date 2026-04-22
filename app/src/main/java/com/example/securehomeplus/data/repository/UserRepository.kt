package com.example.securehomeplus.data.repository

import android.util.Log
import com.example.securehomeplus.data.database.dao.UserDao
import com.example.securehomeplus.data.database.entities.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class UserRepository(private val userDao: UserDao) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    suspend fun registerUser(user: User): Result<Boolean> {
        return try {
            val result = auth.createUserWithEmailAndPassword(user.email, user.password).await()
            if (result.user != null) {
                val profile = mapOf("name" to user.name, "email" to user.email)
                database.getReference("users").child(result.user!!.uid).setValue(profile).await()
                userDao.insertUser(user)
                Result.success(true)
            } else {
                Result.failure(Exception("Registration failed: User is null"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseError", "Register Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val dataSnapshot = database.getReference("users").child(firebaseUser.uid).get().await()
                val name = dataSnapshot.child("name").value as? String ?: "User"
                val user = User(name = name, email = email, password = password)
                userDao.insertUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Login failed: User is null"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseError", "Login Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun isUserRegistered(email: String): Boolean {
        return userDao.getUserByEmail(email) != null
    }

    fun logout() {
        auth.signOut()
    }
}
