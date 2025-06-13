package me.neko.nzhelper.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.core.content.edit
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.OutputStreamWriter

@SuppressLint("DefaultLocale")
private fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append(String.format("%02d:", hours))
        append(String.format("%02d:%02d", minutes, seconds))
    }
}

data class Session(val timestamp: LocalDateTime, val duration: Int, val remark: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sessions_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE_TIME }
    val sessions = remember { mutableStateListOf<Session>() }

    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }

    // 导出 Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    // 序列化 [time, duration, remark]
                    val outList = sessions.map { s ->
                        listOf(s.timestamp.format(formatter), s.duration, s.remark)
                    }
                    writer.write(gson.toJson(outList))
                }
            }
        }
    }

    // 导入 Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.bufferedReader()?.use(BufferedReader::readText)?.let { jsonIn ->
                // 兼容新版三元组
                val root: JsonArray = JsonParser.parseString(jsonIn).asJsonArray
                sessions.clear()
                root.forEach { elem: JsonElement ->
                    if (elem.isJsonArray) {
                        val arr = elem.asJsonArray
                        val timeStr = arr[0].asString
                        val dur = arr[1].asInt
                        val rem = if (arr.size() >= 3 && !arr[2].isJsonNull) arr[2].asString else ""
                        sessions.add(Session(LocalDateTime.parse(timeStr, formatter), dur, rem))
                    }
                }
                prefs.edit { putString("sessions", gson.toJson(root)) }
            }
        }
    }

    // 读取历史（兼容旧版）
    LaunchedEffect(Unit) {
        val jsonStr = prefs.getString("sessions", "[]") ?: "[]"
        val root: JsonArray = JsonParser.parseString(jsonStr).asJsonArray
        sessions.clear()
        root.forEach { elem: JsonElement ->
            if (elem.isJsonArray) {
                val arr = elem.asJsonArray
                val timeStr = arr[0].asString
                val dur = arr[1].asInt
                val rem = if (arr.size() >= 3 && !arr[2].isJsonNull) arr[2].asString else ""
                sessions.add(Session(LocalDateTime.parse(timeStr, formatter), dur, rem))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出数据") },
                            onClick = {
                                showMenu = false
                                exportLauncher.launch("NzHelper_export.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入数据") },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("清除全部记录") },
                            onClick = {
                                showMenu = false
                                showClearDialog = true
                            }
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            if (sessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无历史记录", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(sessions) { session ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("时间: ${session.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
                                        Text("持续: ${formatTime(session.duration)}")
                                        if (session.remark.isNotEmpty()) {
                                            Text("备注: ${session.remark}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    IconButton(onClick = { sessionToDelete = session }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除记录")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (sessionToDelete != null) {
                AlertDialog(
                    onDismissRequest = { sessionToDelete = null },
                    title = { Text("删除记录") },
                    text = { Text("确认删除此记录？") },
                    confirmButton = {
                        TextButton(onClick = {
                            sessions.remove(sessionToDelete)
                            prefs.edit {putString("sessions", gson.toJson(sessions.map { listOf(it.timestamp.format(formatter), it.duration, it.remark) }))}
                            sessionToDelete = null
                        }) { Text("确认") }
                    },
                    dismissButton = {
                        TextButton(onClick = { sessionToDelete = null }) { Text("取消") }
                    }
                )
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("清除全部记录") },
                    text = { Text("确认要清除所有历史记录吗？此操作不可撤销。") },
                    confirmButton = {
                        TextButton(onClick = {
                            sessions.clear()
                            prefs.edit {remove("sessions")}
                            showClearDialog = false
                        }) { Text("删除") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) { Text("取消") }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}
