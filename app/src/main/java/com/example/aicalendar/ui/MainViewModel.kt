package com.example.aicalendar.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicalendar.data.preferences.PreferencesManager
import com.example.aicalendar.domain.calendar.CalendarManager
import com.example.aicalendar.domain.calendar.ICalendarGenerator
import com.example.aicalendar.domain.model.EventData
import com.example.aicalendar.domain.parser.TextParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val textParser: TextParser,
    private val calendarGenerator: ICalendarGenerator,
    private val calendarManager: CalendarManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val apiKey = preferencesManager.apiKey

    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesManager.saveApiKey(apiKey)
        }
    }

    fun processText(text: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                textParser.parseText(text).fold(
                    onSuccess = { eventData ->
                        val calendar = calendarGenerator.generateICalendar(eventData)
                        val file = File(
                            context.getExternalFilesDir(null),
                            "event.ics"
                        )
                        calendarGenerator.saveToFile(calendar, file)
                        _uiState.value = UiState.Success(file.absolutePath, eventData)
                    },
                    onFailure = { error ->
                        _uiState.value = UiState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addToSystemCalendar(eventData: EventData) {
        calendarManager.addEventToSystemCalendar(eventData)
    }

    fun openIcsFile(filePath: String) {
        calendarManager.openIcsFile(File(filePath))
    }

    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        data class Success(val filePath: String, val eventData: EventData) : UiState()
        data class Error(val message: String) : UiState()
    }
} 