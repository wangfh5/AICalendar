package com.example.aicalendar.domain.calendar

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VAlarm
import biweekly.component.VEvent
import biweekly.property.*
import biweekly.util.Duration
import com.example.aicalendar.domain.model.EventData
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ICalendarGenerator @Inject constructor() {
    
    /**
     * Generate an iCalendar file from event data
     */
    fun generateICalendar(eventData: EventData): ICalendar {
        val ical = ICalendar()
        val event = VEvent()
        
        // Set basic properties
        event.setSummary(eventData.summary)
        event.setDateStart(eventData.startTime)
        event.setDateEnd(eventData.endTime)
        
        // Add location if available
        eventData.location?.let { location ->
            event.setLocation(location)
        }
        
        // Add description if available
        eventData.description?.let { description ->
            event.setDescription(description)
        }
        
        // Add attendees
        eventData.attendees.forEach { email ->
            event.addAttendee(email)
        }
        
        // Add reminder
        if (eventData.reminderMinutes > 0) {
            val triggerTime = Calendar.getInstance().apply {
                time = eventData.startTime
                add(Calendar.MINUTE, -eventData.reminderMinutes)
            }.time
            val alarm = VAlarm(Action.display(), Trigger(triggerTime))
            alarm.addProperty(Description(eventData.summary))
            event.addAlarm(alarm)
        }
        
        ical.addEvent(event)
        return ical
    }
    
    /**
     * Save calendar to a file
     */
    fun saveToFile(calendar: ICalendar, file: File) {
        FileOutputStream(file).use { out ->
            Biweekly.write(calendar).go(out)
        }
    }
} 