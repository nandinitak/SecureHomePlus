package com.example.securehomeplus.ui.ai

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securehomeplus.databinding.ActivityHealthGiniBinding

class HealthGiniActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHealthGiniBinding
    private val viewModel: GeminiViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthGiniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupChat()
        setupListeners()
        observeViewModel()
    }

    private fun setupChat() {
        adapter = ChatAdapter(viewModel.messages.value ?: mutableListOf())
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            adapter.notifyDataSetChanged()
            if (messages.isNotEmpty()) {
                binding.rvChat.smoothScrollToPosition(messages.size - 1)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSend.isEnabled = !isLoading
        }
    }
}
