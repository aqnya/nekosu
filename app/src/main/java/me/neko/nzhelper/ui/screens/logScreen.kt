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
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // 记住 logcat 进程
    var process: Process? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        try {
            process = withContext(Dispatchers.IO) {
                Runtime.getRuntime().exec("logcat")
            }

            val reader = BufferedReader(InputStreamReader(process!!.inputStream))
            withContext(Dispatchers.IO) {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        withContext(Dispatchers.Main) {
                            logs = (logs + it).takeLast(500)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logs = listOf("无法读取日志: ${e.message}")
        }
    }

    // 离开界面时销毁 logcat 进程
    DisposableEffect(Unit) {
        onDispose {
            process?.destroy()
            process = null
        }
    }

    LaunchedEffect(logs) {
    if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
    }
}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logcat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                IconButton(onClick = { exportLogsToDownloads(context, logs) }) {
    Icon(
        imageVector = Icons.Default.Save,
        contentDescription = "导出日志"
    )
}
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

private fun exportLogsToDownloads(context: Context, logs: List<String>) {
    if (logs.isEmpty()) return

    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val filename = "logcat_${sdf.format(Date())}.txt"

    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloadsDir.exists()) {
        downloadsDir.mkdirs()
    }

    val file = File(downloadsDir, filename)
    try {
        FileOutputStream(file).use { fos ->
            logs.forEach { line ->
                fos.write((line + "\n").toByteArray())
            }
        }
        Toast.makeText(context, "日志已导出到下载目录: $filename", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "导出日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}