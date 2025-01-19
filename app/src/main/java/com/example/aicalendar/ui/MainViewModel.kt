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
import kotlinx.coroutines.withTimeoutOrNull

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

    private val _textLength = MutableStateFlow(0)
    val textLength: StateFlow<Int> = _textLength.asStateFlow()

    val apiKey = preferencesManager.apiKey

    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesManager.saveApiKey(apiKey)
        }
    }

    fun updateTextLength(text: String) {
        _textLength.value = text.length
    }

    companion object {
        const val MAX_TEXT_LENGTH = 5000  // 最大字数限制
        const val MAX_PROCESSING_TIME = 180  // 最长处理时间(秒)
        const val WARNING_TIME = 120  // 警告时间(秒)
    }

    fun processText(text: String) {
        if (text.length > MAX_TEXT_LENGTH) {
            _uiState.value = UiState.Error("文本超过${MAX_TEXT_LENGTH}字限制")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = UiState.Processing("正在分析文本...")
            
            try {
                _uiState.value = UiState.Processing("正在解析事件信息...")
                // 只在API调用时使用timeout
                val parseResult = withTimeoutOrNull(MAX_PROCESSING_TIME * 1000L) {
                    textParser.parseText(text)
                }

                if (parseResult == null) {
                    _uiState.value = UiState.Error("""
                        处理超时，可能的原因：
                        1. 网络连接不稳定
                        2. API服务器响应较慢
                        3. 文本内容过于复杂
                        
                        建议：
                        1. 检查网络连接
                        2. 尝试分段处理（每段1000字左右）
                        3. 稍后重试
                    """.trimIndent())
                    return@launch
                }

                parseResult.fold(
                    onSuccess = { eventData ->
                        _uiState.value = UiState.Processing("正在生成日历文件...")
                        val calendar = calendarGenerator.generateICalendar(eventData)
                        val file = File(
                            context.getExternalFilesDir(null),
                            "event.ics"
                        )
                        calendarGenerator.saveToFile(calendar, file)
                        _uiState.value = UiState.Success(file.absolutePath, eventData)
                    },
                    onFailure = { error ->
                        _uiState.value = UiState.Error("""
                            处理失败：${error.message}
                            
                            可能的原因：
                            1. API密钥无效或过期
                            2. 文本格式不正确
                            3. 服务器错误
                            
                            建议：
                            1. 检查API密钥设置
                            2. 确保文本包含完整的事件信息
                            3. 稍后重试
                        """.trimIndent())
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("""
                    发生错误：${e.message}
                    
                    可能的原因：
                    1. 网络连接问题
                    2. 应用内部错误
                    
                    建议：
                    1. 检查网络连接
                    2. 重启应用后重试
                    3. 如果问题持续，请联系开发者
                """.trimIndent())
            }
        }
    }

    fun cancelProcessing() {
        viewModelScope.launch {
            _uiState.value = UiState.Error("处理时间超过${MAX_PROCESSING_TIME}秒，请尝试缩短文本或分段处理")
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
        data class Processing(val progress: String) : UiState()
        data class Success(val filePath: String, val eventData: EventData) : UiState()
        data class Error(val message: String) : UiState()
    }
} 