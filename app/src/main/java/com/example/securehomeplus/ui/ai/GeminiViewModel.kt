package com.example.securehomeplus.ui.ai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiViewModel : ViewModel() {

    private val _messages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val messages: LiveData<MutableList<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val API_KEY = "AIzaSyAO7TYvZ1xPe_Mt_EGNSj3gASgbdmdHohk"
    private val MODEL = "gemini-1.5-flash"
    private val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$API_KEY"

    private val SYSTEM_PROMPT = """
        You are 'Health Gini', a specialized AI assistant for the SecureHome+ app.
        Your expertise is Home Security and Home Health.
        Be friendly, professional, and safety-conscious.
        If a user asks about security, provide tips on locks, cameras, and hazard prevention.
        If a user asks about health, provide first-aid advice or home safety tips for children/elderly.
        Always remind users that for real emergencies, they should call 112 or use the Emergency Alert button in the app.
        Keep your responses concise and helpful.
    """.trimIndent()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Keep track of conversation history for context
    private val conversationHistory = mutableListOf<Pair<String, String>>() // (role, text)

    init {
        _messages.value?.add(
            ChatMessage(
                "Hello! I am Health Gini, your personal home security and health assistant. How can I help you today? 🏠🛡️",
                false
            )
        )
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        // Add user message to UI
        _messages.value?.add(ChatMessage(userText, true))
        _messages.value = _messages.value

        // Add to history
        conversationHistory.add(Pair("user", userText))

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    callGeminiApi(userText)
                }
                // Add to history
                conversationHistory.add(Pair("model", response))
                _messages.value?.add(ChatMessage(response, false))
                _messages.value = _messages.value
            } catch (e: Exception) {
                _messages.value?.add(
                    ChatMessage("Sorry, I couldn't get a response. Please check your internet connection and try again.", false)
                )
                _messages.value = _messages.value
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun callGeminiApi(userText: String): String {
        // Build contents array with system prompt + history
        val contentsArray = JSONArray()

        // Add system instruction as first user turn for better compatibility
        val systemTurn = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", "System: $SYSTEM_PROMPT")))
        }
        contentsArray.put(systemTurn)

        val systemAckTurn = JSONObject().apply {
            put("role", "model")
            put("parts", JSONArray().put(JSONObject().put("text", "Understood! I am Health Gini, ready to help with home security and health advice.")))
        }
        contentsArray.put(systemAckTurn)

        // Add conversation history
        for ((role, text) in conversationHistory) {
            val turn = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            }
            contentsArray.put(turn)
        }

        val requestBody = JSONObject().apply {
            put("contents", contentsArray)
        }.toString()

        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        val jsonResponse = JSONObject(responseBody)

        // Check for API errors
        if (jsonResponse.has("error")) {
            val error = jsonResponse.getJSONObject("error")
            throw Exception(error.getString("message"))
        }

        // Extract response text
        return jsonResponse
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }
}
