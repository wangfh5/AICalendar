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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Singleton
class TextParser @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val timezone = ZoneId.systemDefault()
    
    private fun getSystemPrompt(): String {
        val currentDate = ZonedDateTime.now(timezone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return """You are a calendar event parsing assistant. Your task is to extract event information from natural language descriptions.

Please extract the following information in JSON format:
{
    "summary": "Event title",
    "startTime": "YYYY-MM-DDTHH:mm:ss",
    "endTime": "YYYY-MM-DDTHH:mm:ss",
    "location": "Location",
    "description": "Detailed event information, excluding time/location/basic attendee info that are covered by other fields",
    "attendees": ["attendee1@email.com", "attendee2@email.com"],
    "reminderMinutes": reminder time in minutes
}

Today's date is $currentDate.

2025年节假日调休安排：
- 元旦：2024-12-30 至 2025-01-01，共3天
- 春节：2025-01-28 至 2025-02-04，共8天
- 清明节：2025-04-05 至 2025-04-07，共3天
- 劳动节：2025-05-01 至 2025-05-05，共5天
- 端午节：2025-05-31 至 2025-06-02，共3天
- 中秋节：2025-10-06，共1天（与国庆节连休）
- 国庆节：2025-10-01 至 2025-10-08，共8天

Rules:
1. Language matching (IMPORTANT):
   - For Chinese input (contains any Chinese characters), MUST output summary and description in Chinese
   - For English input (no Chinese characters), output summary and description in English
   - Examples for Chinese input:
     - Input: "明天下午3点在星巴克见面" -> summary: "星巴克见面"
     - Input: "下周一开产品评审会" -> summary: "产品评审会"
   - Examples for English input:
     - Input: "meeting tomorrow 3pm" -> summary: "Meeting"
     - Input: "product review next Monday" -> summary: "Product Review"

2. Time parsing rules:
   - "today" refers to $currentDate
   - "tomorrow" means the next day
   - "next Monday" means the next occurring Monday
   - For dates like "Jan 25th", assume it's in the current year unless specified
   - If only time is given without date, assume today ($currentDate)
   - Parse all times to 24-hour format (e.g., "2pm" -> "14:00")
   - For morning/afternoon without specific time: morning = 9:00, afternoon = 14:00

3. Duration rules:
   - If no end time is specified, assume the event lasts for 1 hour
   - For "lunch" or "dinner" without specified duration, assume 1.5 hours
   - For "meeting" without specified duration, assume 1 hour

4. Location handling:
   - If no location is specified, return null
   - Keep the exact location name as provided
   - For online meetings:
     * Use "Online" (English) or "线上" (Chinese) as location
     * DO NOT remove meeting details from description
     * Meeting details (ID, password, etc.) MUST stay in description

5. Attendee rules:
   - Extract all email addresses as attendees
   - If no attendees are specified, return empty list
   - Include any email addresses mentioned in the description

6. Reminder rules:
   - Default reminder is 15 minutes before
   - Parse explicit reminder times (e.g., "remind me 1 hour before" -> 60)
   - For important meetings/presentations, set default reminder to 30 minutes

7. Title rules:
   - Make the summary concise but informative
   - Include the meeting type (e.g., "Team Meeting"/"团队会议", "Client Meeting"/"客户会议")
   - For academic talks, use the format: "[Series Name] [Talk Title]"

8. Description rules:
   - IMPORTANT: Keep the text AS IS, only remove duplicated basic event info
   - DO NOT try to make the description concise
   - CRITICAL: Meeting connection details MUST be preserved:
     * Meeting platform (Zoom, Teams, Tencent Meeting, 腾讯会议等)
     * Meeting ID/room number
     * Meeting passwords/passcode
     * Meeting links
     * Any other connection instructions
   - DO NOT remove or modify any of these:
     * References and citations
     * Bibliography/reference list
     * Technical details
     * Abstract content
     * Biography
   - Only remove information that is exactly duplicated in other fields:
     * Basic time/date info (but keep timezone information)
     * Basic location info (but keep online meeting details)
     * Basic attendee list
   - Keep the original text structure and formatting
     * Add line breaks between sections
     * For Bibliography/reference list, each item should be on a new line"""
    }

    /**
     * Parse natural language text into EventData using DeepSeek API
     */
    suspend fun parseText(text: String): Result<EventData> = withContext(Dispatchers.IO) {
        try {
            val apiKey = preferencesManager.apiKey.first()
            val baseUrl = preferencesManager.baseUrl.first()
            val modelName = preferencesManager.modelName.first()
            
            if (apiKey.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("请先设置API密钥"))
            }

            val prompt = getSystemPrompt()
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", prompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", modelName)
                put("messages", messages)
                put("temperature", 0.1)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(Exception(
                    "API请求失败: ${response.code} ${response.message}\n" +
                    "响应内容: $responseBody"
                ))
            }

            val jsonResponse = JSONObject(responseBody)
            val content = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            val eventJson = JSONObject(content)
            val eventData = EventData(
                summary = eventJson.getString("summary"),
                startTime = dateFormat.parse(eventJson.getString("startTime"))!!,
                endTime = dateFormat.parse(eventJson.getString("endTime"))!!,
                location = if (eventJson.isNull("location")) null else eventJson.getString("location"),
                description = if (eventJson.isNull("description")) null else eventJson.getString("description"),
                attendees = mutableListOf<String>().apply {
                    if (!eventJson.isNull("attendees")) {
                        val attendeesArray = eventJson.getJSONArray("attendees")
                        for (i in 0 until attendeesArray.length()) {
                            add(attendeesArray.getString(i))
                        }
                    }
                },
                reminderMinutes = if (eventJson.isNull("reminderMinutes")) 15 else eventJson.getInt("reminderMinutes")
            )

            Result.success(eventData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 