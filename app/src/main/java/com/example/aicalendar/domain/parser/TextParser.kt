package com.example.aicalendar.domain.parser

import com.example.aicalendar.data.preferences.PreferencesManager
import com.example.aicalendar.domain.model.EventData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextParser @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    private val client = OkHttpClient()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    
    /**
     * Parse natural language text into EventData using DeepSeek API
     */
    suspend fun parseText(text: String): Result<EventData> = withContext(Dispatchers.IO) {
        try {
            val apiKey = preferencesManager.apiKey.first()
            if (apiKey.isNullOrEmpty()) {
                return@withContext Result.failure(IllegalStateException("API key not set"))
            }
            
            val prompt = """
                Parse the following text into a calendar event. Return a JSON object with these fields:
                - summary: event title/summary
                - startTime: start time in ISO format (YYYY-MM-DDTHH:mm:ss)
                - endTime: end time in ISO format
                - description: optional description
                - location: optional location
                - attendees: array of email addresses
                - reminderMinutes: minutes before event to send reminder (default 0)
                
                Text to parse: $text
            """.trimIndent()
            
            val requestBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 1000)
            }.toString()
            
            val request = Request.Builder()
                .url("https://api.deepseek.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("API call failed (${response.code}): $errorBody"))
                }
                
                val responseJson = JSONObject(response.body?.string() ?: "")
                val content = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    
                // 从content中提取JSON字符串
                val jsonStart = content.indexOf("{")
                val jsonEnd = content.lastIndexOf("}") + 1
                if (jsonStart == -1 || jsonEnd == -1) {
                    return@withContext Result.failure(Exception("Invalid response format: $content"))
                }
                
                val eventJsonStr = content.substring(jsonStart, jsonEnd)
                try {
                    val eventJson = JSONObject(eventJsonStr)
                    
                    val eventData = EventData(
                        summary = eventJson.getString("summary"),
                        startTime = dateFormat.parse(eventJson.getString("startTime"))!!,
                        endTime = dateFormat.parse(eventJson.getString("endTime"))!!,
                        description = eventJson.optString("description").takeIf { it.isNotEmpty() },
                        location = eventJson.optString("location").takeIf { it.isNotEmpty() },
                        attendees = eventJson.optJSONArray("attendees")?.let { array ->
                            List(array.length()) { array.getString(it) }
                        } ?: emptyList(),
                        reminderMinutes = eventJson.optInt("reminderMinutes", 0)
                    )
                    
                    Result.success(eventData)
                } catch (e: Exception) {
                    Result.failure(Exception("Failed to parse event data: ${e.message}\nContent: $content"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 