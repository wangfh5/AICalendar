package com.example.aicalendar.di

import android.app.Application
import android.content.Context
import com.example.aicalendar.data.preferences.PreferencesManager
import com.example.aicalendar.domain.calendar.CalendarManager
import com.example.aicalendar.domain.calendar.ICalendarGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideApplicationContext(application: Application): Context = application.applicationContext
    
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
    
    @Provides
    @Singleton
    fun provideICalendarGenerator(): ICalendarGenerator {
        return ICalendarGenerator()
    }
    
    @Provides
    @Singleton
    fun provideCalendarManager(@ApplicationContext context: Context): CalendarManager {
        return CalendarManager(context)
    }
}
