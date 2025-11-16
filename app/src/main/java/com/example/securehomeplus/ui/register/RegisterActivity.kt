//package com.example.securehomeplus.ui.register
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.ViewModelProvider
//import com.example.securehomeplus.data.database.AppDatabase
//import com.example.securehomeplus.data.repository.UserRepository
//import com.example.securehomeplus.databinding.ActivityRegisterBinding
//import com.example.securehomeplus.ui.login.LoginActivity
//import com.example.securehomeplus.utils.ValidationUtils
//import com.example.securehomeplus.viewmodel.AuthViewModel
//import com.example.securehomeplus.viewmodel.AuthViewModelFactory
//
//class RegisterActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityRegisterBinding
//    private lateinit var viewModel: AuthViewModel
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        binding = ActivityRegisterBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//
//
//        // ViewModel setup
//        val dao = AppDatabase.getDatabase(this).userDao()
//        val repository = UserRepository(dao)
//        val factory = AuthViewModelFactory(repository)
//        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
//
//        // 🔹 Register button click
//        binding.btnRegister.setOnClickListener {
//            val name = binding.etName.text.toString().trim()
//            val email = binding.etEmail.text.toString().trim()
//            val password = binding.etPassword.text.toString().trim()
//
//            when {
//                !ValidationUtils.isNotEmpty(name) -> binding.etName.error = "Enter your name"
//                !ValidationUtils.isValidEmail(email) -> binding.etEmail.error = "Enter valid email"
//                !ValidationUtils.isValidPassword(password) -> binding.etPassword.error =
//                    "Password must be at least 6 characters"
//                else -> {
//                    viewModel.registerUser(name, email, password)
//                }
//            }
//        }
//
//        // 🔹 Observe register result
//        viewModel.registerResult.observe(this) { success ->
//            if (success) {
//                Toast.makeText(this, "Registered Successfully!", Toast.LENGTH_SHORT).show()
//                startActivity(Intent(this, LoginActivity::class.java))
//                finish()
//            } else {
//                Toast.makeText(this, "User already exists!", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        // 🔹 Already have account click
//        binding.tvLogin.setOnClickListener {
//            startActivity(Intent(this, LoginActivity::class.java))
//            finish()
//        }
//    }
//}


package com.example.securehomeplus.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.securehomeplus.data.database.AppDatabase
import com.example.securehomeplus.data.repository.UserRepository
import com.example.securehomeplus.databinding.ActivityRegisterBinding
import com.example.securehomeplus.ui.login.LoginActivity
import com.example.securehomeplus.utils.ValidationUtils
import com.example.securehomeplus.viewmodel.AuthViewModel
import com.example.securehomeplus.viewmodel.AuthViewModelFactory

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupRegisterButton()

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupViewModel() {
        val dao = AppDatabase.getDatabase(this).userDao()
        val repo = UserRepository(dao)
        val factory = AuthViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        viewModel.registerResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Registered Successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "User already exists!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRegisterButton() {
        binding.btnRegister.setOnClickListener {

            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()

            when {
                !ValidationUtils.isNotEmpty(name) -> binding.etName.error = "Enter your name"
                !ValidationUtils.isValidEmail(email) -> binding.etEmail.error = "Invalid email"
                !ValidationUtils.isValidPassword(password) ->
                    binding.etPassword.error = "Password must be at least 6 characters"
                confirm != password -> binding.etConfirmPassword.error = "Passwords do not match"
                else -> viewModel.registerUser(name, email, password)
            }
        }
    }
}
