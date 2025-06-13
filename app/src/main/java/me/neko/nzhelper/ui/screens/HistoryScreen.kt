package me.neko.nzhelper.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParser
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    var sessionToView by remember { mutableStateOf<Session?>(null) }

    // 导出 Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    // 序列化
                    val outList = sessions.map { s ->
                        listOf(
                            s.timestamp.format(formatter),
                            s.duration,
                            s.remark,
                            s.location,
                            s.watchedMovie,
                            s.climax,
                            s.rating,
                            s.mood,
                            s.props
                        )
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
            context.contentResolver.openInputStream(it)
                ?.bufferedReader()
                ?.use { reader ->
                    val jsonStr = reader.readText()
                    val root = JsonParser.parseString(jsonStr).asJsonArray

                    sessions.clear()

                    for (elem in root) {
                        if (elem.isJsonArray) {
                            val arr = elem.asJsonArray
                            val timeStr = arr[0].asString
                            val dur = if (arr.size() >= 2) arr[1].asInt else 0
                            val rem =
                                if (arr.size() >= 3 && !arr[2].isJsonNull) arr[2].asString else ""
                            val loc =
                                if (arr.size() >= 4 && !arr[3].isJsonNull) arr[3].asString else ""
                            val watched = if (arr.size() >= 5) arr[4].asBoolean else false
                            val climaxed = if (arr.size() >= 6) arr[5].asBoolean else false
                            val rate = if (arr.size() >= 7 && !arr[6].isJsonNull) {
                                arr[6].asFloat.coerceIn(0f, 5f) // 确保在范围内
                            } else 0f
                            val md = if (arr.size() >= 8 && !arr[7].isJsonNull) arr[7].asString else ""
                            val prop = if (arr.size() >= 9 && !arr[8].isJsonNull) arr[8].asString else ""
                            sessions.add(
                                Session(
                                    timestamp = LocalDateTime.parse(timeStr, formatter),
                                    duration = dur,
                                    remark = rem,
                                    location = loc,
                                    watchedMovie = watched,
                                    climax = climaxed,
                                    rating = rate,
                                    mood = md,
                                    props = prop
                                )
                            )
                        }
                    }

                    prefs.edit {
                        putString("sessions", jsonStr)
                    }
                }
        }
    }

    // 读取历史（兼容旧版）
    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                            onClick = { sessionToView = session }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            "时间: ${
                                                session.timestamp.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "yyyy-MM-dd HH:mm:ss"
                                                    )
                                                )
                                            }"
                                        )
                                        Text("持续: ${formatTime(session.duration)}")
                                        if (session.remark.isNotEmpty()) {
                                            Text(
                                                "备注: ${session.remark}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
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
                            prefs.edit {
                                putString(
                                    "sessions",
                                    gson.toJson(sessions.map {
                                        listOf(
                                            it.timestamp.format(formatter),
                                            it.duration,
                                            it.remark
                                        )
                                    })
                                )
                            }
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
                            prefs.edit { remove("sessions") }
                            showClearDialog = false
                        }) { Text("删除") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) { Text("取消") }
                    }
                )
            }

            // 查看详情对话框
            if (sessionToView != null) {
                AlertDialog(
                    onDismissRequest = { sessionToView = null },
                    title = { Text("会话详情") },
                    text = {
                        Column {
                            val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            Text("开始时间：${sessionToView!!.timestamp.format(pattern)}")
                            Text("持续时长：${formatTime(sessionToView!!.duration)}")
                            if (sessionToView!!.remark.isNotEmpty()) {
                                Text("备注：${sessionToView!!.remark}")
                            } else {
                                Text("备注：无")
                            }
                            if (sessionToView!!.location.isNotEmpty()) {
                                Text("地点：${sessionToView!!.location}")
                            } else {
                                Text("地点：无")
                            }
                            Text("观看小电影：${if (sessionToView!!.watchedMovie) "是" else "否"}")
                            Text("高潮：${if (sessionToView!!.climax) "是" else "否"}")
                            if (sessionToView!!.props.isNotEmpty())
                                Text("道具：${sessionToView!!.props}")
                            Text("评分：${"%.1f".format(sessionToView!!.rating)} / 5.0")
                            if (sessionToView!!.mood.isNotEmpty())
                                Text("心情：${sessionToView!!.mood}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { sessionToView = null }) {
                            Text("关闭")
                        }
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
