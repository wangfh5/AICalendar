package com.example.aicalendar.domain.calendar

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.aicalendar.domain.model.EventData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun addEventToSystemCalendar(eventData: EventData) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, eventData.summary)
            putExtra(CalendarContract.Events.DESCRIPTION, eventData.description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, eventData.location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventData.startTime.time)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventData.endTime.time)
            putExtra(CalendarContract.Events.HAS_ALARM, eventData.reminderMinutes > 0)
            if (eventData.reminderMinutes > 0) {
                putExtra(CalendarContract.Reminders.MINUTES, eventData.reminderMinutes)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openIcsFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/calendar")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "打开日历文件").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
