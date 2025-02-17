package com.example.aicalendar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aicalendar.ui.MainViewModel.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // 处理权限被拒绝的情况
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(
                this,
                writePermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(writePermission)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle(initialValue = null)
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle(initialValue = "https://api.openai.com/v1")
    val modelName by viewModel.modelName.collectAsStateWithLifecycle(initialValue = "gpt-3.5-turbo")
    val textLength by viewModel.textLength.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // API Settings Dialog
        if (showApiKeyDialog) {
            var apiKeyInput by remember { mutableStateOf(apiKey ?: "") }
            var baseUrlInput by remember { mutableStateOf(baseUrl) }
            var modelNameInput by remember { mutableStateOf(modelName) }
            
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                title = { Text("API设置") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // API Key
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("API密钥") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Base URL
                        OutlinedTextField(
                            value = baseUrlInput,
                            onValueChange = { baseUrlInput = it },
                            label = { Text("Base URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Model Name
                        OutlinedTextField(
                            value = modelNameInput,
                            onValueChange = { modelNameInput = it },
                            label = { Text("模型名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateApiKey(apiKeyInput)
                        viewModel.updateBaseUrl(baseUrlInput)
                        viewModel.updateModelName(modelNameInput)
                        showApiKeyDialog = false
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApiKeyDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
        
        // Settings Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showApiKeyDialog = true }) {
                Text("设置")
            }
        }
        
        // 可滚动的文本输入区域
        OutlinedTextField(
            value = inputText,
            onValueChange = { 
                inputText = it
                viewModel.updateTextLength(it)
            },
            modifier = Modifier
                .weight(1f)  // 占用剩余空间
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),  // 添加垂直滚动
            label = { Text("输入事件描述") },
            textStyle = TextStyle(fontSize = 16.sp),
            maxLines = Int.MAX_VALUE  // 允许多行输入
        )

        // 字数统计显示
        Text(
            text = "$textLength/${MainViewModel.MAX_TEXT_LENGTH}",
            color = if (textLength > MainViewModel.MAX_TEXT_LENGTH) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall
        )

        // 固定在底部的按钮
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    viewModel.processText(inputText)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("生成日历事件")
        }

        // 显示状态
        when (val state = uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator()
            }
            is UiState.Processing -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.progress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            is UiState.Success -> {
                Text(
                    "日历文件已生成: ${state.filePath}",
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { viewModel.addToSystemCalendar(state.eventData) }) {
                        Text("添加到系统日历")
                    }
                    Button(onClick = { viewModel.openIcsFile(state.filePath) }) {
                        Text("打开ICS文件")
                    }
                }
            }
            is UiState.Error -> {
                Text(
                    "错误: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
} 