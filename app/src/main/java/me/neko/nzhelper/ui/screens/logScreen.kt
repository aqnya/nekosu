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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen() {
    var logs by remember { mutableStateOf(listOf<String>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        try {
            val output = withContext(Dispatchers.IO) {
                val process = Runtime.getRuntime().exec("logcat -d")
                process.inputStream.bufferedReader().use { it.readLines().reversed() }
            }
            logs = output
        } catch (e: Exception) {
            logs = listOf("无法读取日志: ${e.message}")
        }
    }

    LaunchedEffect(logs) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logcat") }
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