package me.neko.nzhelper.ui.screens

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.widget.CalendarView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParser
import me.neko.nzhelper.ui.service.TimerService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sessions_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE_TIME }

    // 绑定 Service
    val serviceIntent = remember { Intent(context, TimerService::class.java) }
    var timerService by remember { mutableStateOf<TimerService?>(null) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                timerService = (binder as TimerService.LocalBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
            }
        }
    }

    // 启动并绑定服务
    LaunchedEffect(Unit) {
        ContextCompat.startForegroundService(
            context,
            serviceIntent.apply { action = TimerService.ACTION_START }
        )
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }
    DisposableEffect(Unit) {
        onDispose { context.unbindService(connection) }
    }

    // 订阅 elapsedSec
    val elapsedSeconds by timerService
        ?.elapsedSec
        ?.collectAsState(initial = 0)
        ?: remember { mutableIntStateOf(0) }

    var isRunning by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showRemarkDialog by remember { mutableStateOf(false) }
    var remarkInput by remember { mutableStateOf("") }

    var showCalendar by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // 会话数据类
    data class Session(
        val timestamp: LocalDateTime,
        val duration: Int,
        val remark: String
    )

    val sessions = remember { mutableStateListOf<Session>() }

    // 加载历史
    LaunchedEffect(Unit) {
        val jsonStr = prefs.getString("sessions", "[]") ?: "[]"
        val root = JsonParser.parseString(jsonStr).asJsonArray
        for (elem in root) {
            if (elem.isJsonArray) {
                val arr = elem.asJsonArray
                val timeStr = arr[0].asString
                val dur = arr[1].asInt
                val rem = if (arr.size() >= 3 && !arr[2].isJsonNull) arr[2].asString else ""
                sessions.add(
                    Session(
                        LocalDateTime.parse(timeStr, formatter),
                        dur,
                        rem
                    )
                )
            }
        }
    }

    // 控制 Service 启停
    LaunchedEffect(isRunning) {
        val action = if (isRunning) TimerService.ACTION_START else TimerService.ACTION_PAUSE
        context.startService(serviceIntent.apply { this.action = action })
    }

    // 检查通知权限
    val isPreview = LocalInspectionMode.current
    val notificationsEnabled = if (isPreview) {
        true // 预览时跳过检查
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // 打开应用通知设置
    fun openNotificationSettings() {
        val intent = Intent().apply {
            action =
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra("app_uid", context.applicationInfo.uid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "牛子小助手") },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // 统计展示
            val totalCount = sessions.size
            val averageTimeMin = if (totalCount > 0)
                sessions.sumOf { it.duration }.toDouble() / totalCount / 60
            else 0.0
            val now = LocalDateTime.now()
            val weekCount = sessions.count { it.timestamp.isAfter(now.minusWeeks(1)) }
            val monthCount = sessions.count { it.timestamp.isAfter(now.minusMonths(1)) }
            val yearCount = sessions.count { it.timestamp.isAfter(now.minusYears(1)) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                if (!notificationsEnabled) {
                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .fillMaxWidth(0.9f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "还未开启通知权限",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Button(
                                        onClick = { openNotificationSettings() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text("去开启")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(
                            text = "记录新的手艺活",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "准备开始",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = formatTime(elapsedSeconds),
                            style = MaterialTheme.typography.displayMedium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = {
                                if (!isRunning) showRemarkDialog = true
                                else isRunning = false
                            }) {
                                Text(if (isRunning) "暂停" else "开始")
                            }
                            Button(onClick = {
                                if (elapsedSeconds > 0) showConfirmDialog = true
                                else Toast.makeText(context, "计时尚未开始", Toast.LENGTH_SHORT)
                                    .show()
                            }) {
                                Text("结束")
                            }
                        }
                    }
                }

                val stats = listOf(
                    "总次数: $totalCount 次",
                    "平均时间: ${"%.1f".format(averageTimeMin)} 分钟",
                    "本周次数: $weekCount 次",
                    "本月次数: $monthCount 次",
                    "今年次数: $yearCount 次"
                )
                items(stats) { info ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clickable {
                                selectedDate = null
                                showCalendar = true
                            },
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(text = info, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // 日历查看对话框
            if (showCalendar) {
                AlertDialog(
                    onDismissRequest = { showCalendar = false },
                    title = { Text(text = "查看历史记录") },
                    text = {
                        Column {
                            AndroidView(
                                factory = { ctx ->
                                    CalendarView(ctx).apply {
                                        setOnDateChangeListener { _, year, month, dayOfMonth ->
                                            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                        }
                                        date = Calendar.getInstance().timeInMillis
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            selectedDate?.let { date ->
                                val daySessions =
                                    sessions.filter { it.timestamp.toLocalDate() == date }
                                if (daySessions.isEmpty()) {
                                    Text(
                                        text = "$date 无记录",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    daySessions.forEach { s ->
                                        Text(
                                            text = "${
                                                s.timestamp.toLocalTime()
                                                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                                            } - ${formatTime(s.duration)}${if (s.remark.isNotEmpty()) " 备注: ${s.remark}" else ""}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCalendar = false }) {
                            Text("关闭")
                        }
                    }
                )
            }

            // 备注对话框
            if (showRemarkDialog) {
                AlertDialog(
                    onDismissRequest = { showRemarkDialog = false },
                    title = { Text("备注（可选）") },
                    text = {
                        TextField(
                            value = remarkInput,
                            onValueChange = { remarkInput = it },
                            placeholder = { Text("有什么想说的？") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showRemarkDialog = false
                            isRunning = true
                        }) { Text("确认") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            remarkInput = ""
                            showRemarkDialog = false
                            isRunning = true
                        }) { Text("跳过") }
                    }
                )
            }

            // 结束对话框
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("结束") },
                    text = { Text("要结束对牛牛的爱抚了吗？") },
                    confirmButton = {
                        TextButton(onClick = {
                            val nowTime = LocalDateTime.now()
                            sessions.add(Session(nowTime, elapsedSeconds, remarkInput))
                            val saveList = sessions.map { s ->
                                listOf(
                                    s.timestamp.format(formatter),
                                    s.duration,
                                    s.remark
                                )
                            }
                            prefs.edit { putString("sessions", gson.toJson(saveList)) }
                            isRunning = false
                            remarkInput = ""
                            showConfirmDialog = false
                            // 停止 Service
                            context.startService(
                                serviceIntent.apply { action = TimerService.ACTION_STOP }
                            )
                        }) { Text("燃尽了") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) { Text("再坚持一下") }
                    }
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatTime(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return buildString {
        if (h > 0) append(String.format("%02d:", h))
        append(String.format("%02d:%02d", m, s))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}