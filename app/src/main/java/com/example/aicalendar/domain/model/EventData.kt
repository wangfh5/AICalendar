package com.example.aicalendar.domain.model

import java.util.Date

/**
 * Data class representing calendar event information
 */
data class EventData(
    val summary: String,
    val startTime: Date,
    val endTime: Date,
    val description: String? = null,
    val location: String? = null,
    val attendees: List<String> = emptyList(),
    val reminderMinutes: Int = 0
) 