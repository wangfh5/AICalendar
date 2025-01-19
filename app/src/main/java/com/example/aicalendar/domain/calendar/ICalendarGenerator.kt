package com.example.aicalendar.domain.calendar

import com.example.aicalendar.domain.model.EventData
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.util.RandomUidGenerator
import net.fortuna.ical4j.util.UidGenerator
import java.io.File
import java.io.FileOutputStream
import java.time.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.parameter.TzId
import net.fortuna.ical4j.model.property.Action.DISPLAY
import net.fortuna.ical4j.model.Calendar as ICalCalendar
import net.fortuna.ical4j.model.Dur

@Singleton
class ICalendarGenerator @Inject constructor() {
    private val uidGenerator: UidGenerator = RandomUidGenerator()
    private val zoneId = ZoneId.systemDefault()
    
    /**
     * Generate an iCalendar file from event data
     */
    fun generateICalendar(eventData: EventData): ICalCalendar {
        val calendar = ICalCalendar()
        calendar.properties.add(ProdId("-//AI Calendar//iCal4j 3.2//EN"))
        calendar.properties.add(Version.VERSION_2_0)
        calendar.properties.add(CalScale.GREGORIAN)
        
        // Add timezone info for better compatibility
        calendar.properties.add(XProperty("X-WR-TIMEZONE", zoneId.id))
        
        // Create event with timezone
        val event = createEvent(eventData)
        calendar.components.add(event)
        
        return calendar
    }
    
    private fun createEvent(eventData: EventData): VEvent {
        // Convert times to local timezone
        val startDateTime = ZonedDateTime.ofInstant(eventData.startTime.toInstant(), zoneId)
        val endDateTime = ZonedDateTime.ofInstant(eventData.endTime.toInstant(), zoneId)
        
        // Create event with timezone
        val event = VEvent()
        
        // Add start time with timezone
        val dtStart = DtStart(DateTime(startDateTime.toInstant().toEpochMilli()))
        dtStart.parameters.add(TzId(zoneId.id))
        event.properties.add(dtStart)
        
        // Add end time with timezone
        val dtEnd = DtEnd(DateTime(endDateTime.toInstant().toEpochMilli()))
        dtEnd.parameters.add(TzId(zoneId.id))
        event.properties.add(dtEnd)
        
        // Add summary
        event.properties.add(Summary(eventData.summary))
        
        // Add UID
        event.properties.add(uidGenerator.generateUid())
        
        // Add description if available
        eventData.description?.takeUnless { it == "null" }?.let { description ->
            event.properties.add(Description(description))
        }
        
        // Add location if available
        eventData.location?.takeUnless { it == "null" }?.let { location ->
            event.properties.add(Location(location))
        }
        
        // Add attendees
        eventData.attendees.filterNotNull().forEach { email ->
            event.properties.add(Attendee("mailto:$email"))
        }
        
        // Add reminder with relative time
        if (eventData.reminderMinutes > 0) {
            val alarm = VAlarm()
            alarm.properties.add(DISPLAY)
            alarm.properties.add(Description("Reminder for ${eventData.summary}"))
            
            // Create relative trigger time using Dur
            val duration = Dur(0, 0, -eventData.reminderMinutes, 0)  // weeks, days, hours, minutes
            val trigger = Trigger(duration)
            trigger.parameters.add(Value.DURATION)  // 显式指定这是一个持续时间
            alarm.properties.add(trigger)
            
            event.components.add(alarm)
        }
        
        // Add status
        event.properties.add(Status.VEVENT_CONFIRMED)
        
        // Add creation timestamp
        event.properties.add(Created(DateTime(System.currentTimeMillis())))
        
        return event
    }
    
    /**
     * Save calendar to a file
     */
    fun saveToFile(calendar: ICalCalendar, file: File) {
        val outputter = CalendarOutputter()
        FileOutputStream(file).use { out ->
            outputter.output(calendar, out)
        }
    }
} 