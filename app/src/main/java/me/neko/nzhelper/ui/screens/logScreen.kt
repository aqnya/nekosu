package me.neko.nzhelper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen(navController: NavHostController) {
    var logs by remember { mutableStateOf(listOf<LogEntry>()) }
    var isRecording by remember { mutableStateOf(true) }
    var isAutoScroll by remember { mutableStateOf(true) }
    var filterLevel by remember { mutableStateOf(LogLevel.ALL) }
    var searchText by remember { mutableStateOf("") }
    var bufferSize by remember { mutableStateOf(1000) }
    
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var process: Process? by remember { mutableStateOf(null) }
    var recordingJob: Job? by remember { mutableStateOf(null) }

    // 自动滚动到底部
    LaunchedEffect(logs.size, isAutoScroll) {
        if (logs.isNotEmpty() && isAutoScroll) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    // 日志收集逻辑
    LaunchedEffect(isRecording, bufferSize) {
        recordingJob?.cancel()
        
        if (isRecording) {
            recordingJob = launch(Dispatchers.IO) {
                try {
                    process?.destroy()
                    process = Runtime.getRuntime().exec("logcat -v time -b main -b system -b crash")
                    
                    val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                    reader.useLines { lines ->
                        lines.forEach { line ->
                            if (isActive && line.isNotBlank()) {
                                val logEntry = parseLogEntry(line)
                                withContext(Dispatchers.Main) {
                                    logs = (logs + logEntry).takeLast(bufferSize)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        logs = listOf(LogEntry(
                            timestamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
                            level = LogLevel.ERROR,
                            tag = "LogcatScreen",
                            message = "无法启动 logcat: ${e.message}",
                            original = "ERROR: ${e.message}"
                        ))
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recordingJob?.cancel()
            process?.destroy()
        }
    }

    // 过滤日志
    val filteredLogs = remember(logs, filterLevel, searchText) {
        logs.filter { entry ->
            (filterLevel == LogLevel.ALL || entry.level == filterLevel) &&
            (searchText.isEmpty() || 
             entry.tag.contains(searchText, true) || 
             entry.message.contains(searchText, true) ||
             entry.original.contains(searchText, true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logcat 日志查看器") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 自动滚动开关
                    IconButton(
                        onClick = { isAutoScroll = !isAutoScroll },
                        enabled = isRecording
                    ) {
                        Icon(
                            imageVector = if (isAutoScroll) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = if (isAutoScroll) "自动滚动开启" else "自动滚动关闭",
                            tint = if (isAutoScroll && isRecording) MaterialTheme.colorScheme.primary 
                                  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // 暂停 / 恢复
                    IconButton(onClick = { 
                        isRecording = !isRecording 
                    }) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRecording) "暂停" else "继续"
                        )
                    }

                    // 清除日志
                    IconButton(onClick = {
                        logs = emptyList()
                        try {
                            Runtime.getRuntime().exec("logcat -c")
                            Toast.makeText(context, "日志已清除", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "清除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除日志"
                        )
                    }

                    // 导出日志
                    IconButton(
                        onClick = {
                            if (logs.isNotEmpty()) {
                                exportLogsToDownloads(context, logs)
                            } else {
                                Toast.makeText(context, "没有日志可导出", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = logs.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "导出日志"
                        )
                    }
                }
            )
        },
        bottomBar = {
            LogcatBottomBar(
                filterLevel = filterLevel,
                onFilterChange = { filterLevel = it },
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                bufferSize = bufferSize,
                onBufferSizeChange = { bufferSize = it },
                logCount = logs.size,
                filteredCount = filteredLogs.size
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 状态指示器
            LogcatStatusBar(
                isRecording = isRecording,
                isAutoScroll = isAutoScroll,
                filterLevel = filterLevel,
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            logs.isEmpty() -> "暂无日志"
                            searchText.isNotEmpty() -> "未找到包含 \"$searchText\" 的日志"
                            else -> "当前过滤条件下无日志"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(filteredLogs) { logEntry ->
                        LogItem(logEntry = logEntry)
                    }
                }
            }
        }
    }
}

@Composable
fun LogcatStatusBar(
    isRecording: Boolean,
    isAutoScroll: Boolean,
    filterLevel: LogLevel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 录制状态
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isRecording) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.extraSmall
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isRecording) "录制中" else "已暂停",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 自动滚动状态
        if (isAutoScroll) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "自动滚动",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 过滤状态
        if (filterLevel != LogLevel.ALL) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = getLogLevelColor(filterLevel)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = filterLevel.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = getLogLevelColor(filterLevel)
                )
            }
        }
    }
}

@Composable
fun LogcatBottomBar(
    filterLevel: LogLevel,
    onFilterChange: (LogLevel) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    bufferSize: Int,
    onBufferSizeChange: (Int) -> Unit,
    logCount: Int,
    filteredCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        // 搜索和过滤行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 搜索框
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                placeholder = { Text("搜索日志...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { onSearchTextChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除搜索")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )

            // 过滤菜单
            var expanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = filterLevel != LogLevel.ALL,
                    onClick = { expanded = true },
                    label = { 
                        Text(
                            text = filterLevel.displayName, 
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    LogLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = level.displayName,
                                    color = getLogLevelColor(level)
                                )
                            },
                            onClick = {
                                onFilterChange(level)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // 状态信息行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "日志: $filteredCount/$logCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "缓冲区: $bufferSize",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun LogItem(logEntry: LogEntry) {
    val levelColor = getLogLevelColor(logEntry.level)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // 头部：时间戳和标签
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = logEntry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 级别标签
                    Box(
                        modifier = Modifier
                            .background(
                                color = levelColor.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = logEntry.level.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = levelColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // 标签
                    Text(
                        text = logEntry.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 消息内容
            Text(
                text = logEntry.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// 数据类
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val original: String
)

enum class LogLevel(val displayName: String) {
    VERBOSE("V"), DEBUG("D"), INFO("I"), WARN("W"), ERROR("E"), FATAL("F"), ALL("All")
}

// 解析日志行
private fun parseLogEntry(line: String): LogEntry {
    return try {
        // 简化解析，实际应用中可能需要更复杂的正则表达式
        val parts = line.split("\\s+".toRegex())
        if (parts.size >= 5) {
            val timestamp = "${parts[0]} ${parts[1]}"
            val level = when (parts[2]) {
                "V", "VERBOSE" -> LogLevel.VERBOSE
                "D", "DEBUG" -> LogLevel.DEBUG
                "I", "INFO" -> LogLevel.INFO
                "W", "WARN" -> LogLevel.WARN
                "E", "ERROR" -> LogLevel.ERROR
                "F", "FATAL" -> LogLevel.FATAL
                else -> LogLevel.INFO
            }
            val tag = parts[3].removeSuffix(":")
            val message = parts.drop(4).joinToString(" ")
            
            LogEntry(timestamp, level, tag, message, line)
        } else {
            LogEntry(
                timestamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
                level = LogLevel.INFO,
                tag = "System",
                message = line,
                original = line
            )
        }
    } catch (e: Exception) {
        LogEntry(
            timestamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
            level = LogLevel.ERROR,
            tag = "Parser",
            message = "解析失败: $line",
            original = line
        )
    }
}

// 获取日志级别颜色
@Composable
fun getLogLevelColor(level: LogLevel): Color {
    return when (level) {
        LogLevel.VERBOSE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        LogLevel.DEBUG -> Color(0xFF2196F3) // Blue
        LogLevel.INFO -> Color(0xFF4CAF50) // Green
        LogLevel.WARN -> Color(0xFFFF9800) // Orange
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.FATAL -> Color(0xFF9C27B0) // Purple
        LogLevel.ALL -> MaterialTheme.colorScheme.primary
    }
}

// 改进的导出函数
private fun exportLogsToDownloads(context: Context, logs: List<LogEntry>) {
    if (logs.isEmpty()) return

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val filename = "logcat_${sdf.format(Date())}.txt"

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { fos ->
                logs.forEachIndexed { index, entry ->
                    fos.write("${index + 1}. ${entry.original}\n".toByteArray())
                }
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context, 
                    "日志已导出到: ${file.absolutePath}", 
                    Toast.LENGTH_LONG
                ).show()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Preview
@Composable
fun PreviewLogcatScreen() {
    MaterialTheme {
        LogcatScreen(navController = rememberNavController())
    }
}