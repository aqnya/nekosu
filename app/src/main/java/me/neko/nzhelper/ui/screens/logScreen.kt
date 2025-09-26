package me.neko.nzhelper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import android.content.pm.PackageManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import android.content.Context
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen(navController: NavHostController) {
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isRecording by remember { mutableStateOf(true) } // 添加控制状态
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var process: Process? by remember { mutableStateOf(null) }

    // 改进的日志收集逻辑
    LaunchedEffect(isRecording) {
        if (!isRecording) {
            process?.destroy()
            return@LaunchedEffect
        }

        try {
            process = withContext(Dispatchers.IO) {
                // 清除旧日志并开始新的收集
                Runtime.getRuntime().exec("logcat -c")
                Runtime.getRuntime().exec("logcat -v time")
            }

            val reader = BufferedReader(InputStreamReader(process!!.inputStream))
            withContext(Dispatchers.IO) {
                try {
                    reader.lineSequence()
                        .takeWhile { isRecording }
                        .forEach { line ->
                            if (line.isNotBlank()) {
                                withContext(Dispatchers.Main) {
                                    // 限制日志数量，防止内存溢出
                                    logs = (logs + line).takeLast(1000)
                                }
                            }
                        }
                } catch (e: Exception) {
                    if (isRecording) {
                        withContext(Dispatchers.Main) {
                            logs = logs + "日志读取错误: ${e.message}"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logs = listOf("无法启动 logcat: ${e.message}")
        }
    }

    // 自动滚动优化
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && isRecording) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isRecording = false
            process?.destroy()
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
                    // 添加暂停/继续按钮
                    IconButton(onClick = { 
                        isRecording = !isRecording 
                    }) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRecording) "暂停" else "继续"
                        )
                    }
                    
                    // 添加清除日志按钮
                    IconButton(onClick = { 
                        logs = emptyList()
                        try {
                            Runtime.getRuntime().exec("logcat -c")
                        } catch (e: Exception) {
                            Toast.makeText(context, "清除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除日志"
                        )
                    }
                    
                    IconButton(onClick = { 
                        if (logs.isNotEmpty()) {
                            exportLogsToDownloads(context, logs) 
                        } else {
                            Toast.makeText(context, "没有日志可导出", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "导出日志"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无日志", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
        horizontalScroll(rememberScrollState()), // 整体横向滚动
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
            items(logs) { line ->
    LogItem(line = line)
}
            }
        }
    }
}

@Composable
fun LogItem(line: String) {
    val color = when {
        line.contains(" E ", ignoreCase = true) -> Color.Red
        line.contains(" W ", ignoreCase = true) -> Color.Yellow
        line.contains(" I ", ignoreCase = true) -> Color.Green
        line.contains(" D ", ignoreCase = true) -> Color.Blue
        else -> Color.Black
    }

    val hScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
         
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = line,
            style = MaterialTheme.typography.bodySmall.copy(color = color),
            softWrap = false // 保持单行，不换行
        )
    }
}

// 改进的导出函数
private fun exportLogsToDownloads(context: Context, logs: List<String>) {
    if (logs.isEmpty()) return

    try {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val filename = "logcat_${sdf.format(Date())}.txt"

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, filename)
        FileOutputStream(file).use { fos ->
            logs.forEachIndexed { index, line ->
                fos.write("${index + 1}. $line\n".toByteArray())
            }
        }
        
        Toast.makeText(
            context, 
            "日志已导出到: ${file.absolutePath}", 
            Toast.LENGTH_LONG
        ).show()
        
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// 添加权限检查函数（需要在调用前检查权限）
private fun hasWritePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, 
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}

@Preview
@Composable
fun PreviewLogcatScreen() {
    LogcatScreen(navController = rememberNavController())
}
