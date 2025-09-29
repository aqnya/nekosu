package me.neko.nzhelper.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap
)

enum class FilterMode(val label: String) {
    ALL("全部应用"),
    LAUNCHABLE("可启动应用"),
    SYSTEM("系统应用"),
    USER("用户应用")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var filterMode by remember { mutableStateOf(FilterMode.USER) }
    var menuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // 加载所有应用（只在启动时）
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm: PackageManager = context.packageManager
            val installed = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                .mapNotNull { pkgInfo ->
                    pkgInfo.applicationInfo?.let { appInfo ->
                        AppInfo(
                            name = appInfo.loadLabel(pm).toString(),
                            packageName = pkgInfo.packageName,
                            icon = appInfo.loadIcon(pm).toBitmap().asImageBitmap()
                        )
                    }
                }
                .sortedBy { it.name.lowercase() }
            allApps = installed
            apps = installed
            isLoading = false  // 新增：加载完成后关闭 loading
        }
    }

    // 根据过滤器 + 搜索条件动态更新
    LaunchedEffect(filterMode, searchQuery, allApps) {
        val pm: PackageManager = context.packageManager
        apps = allApps.filter { app ->
            // 先按过滤器筛选
            val passFilter = when (filterMode) {
                FilterMode.ALL -> true
                FilterMode.LAUNCHABLE -> pm.getLaunchIntentForPackage(app.packageName) != null
                FilterMode.SYSTEM -> {
                    val appInfo = pm.getApplicationInfo(app.packageName, 0)
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                }
                FilterMode.USER -> {
                    val appInfo = pm.getApplicationInfo(app.packageName, 0)
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
            }
            // 再按搜索关键字过滤
            val query = searchQuery.trim().lowercase()
            val passSearch = query.isEmpty() ||
                app.name.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)

            passFilter && passSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索应用名或包名") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Text(filterMode.label)
                    }
                },
                navigationIcon = {
                    if (isSearching) {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "关闭搜索")
                        }
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "过滤")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            FilterMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = {
                                        filterMode = mode
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            // 新增：三路判断
            if (isLoading) {
                CircularProgressIndicator()
            } else if (apps.isEmpty()) {
                Text("没有找到应用")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(apps) { app ->
                        ListItem(
                            headlineContent = { Text(app.name) },
                            supportingContent = { Text(app.packageName) },
                            leadingContent = {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.name,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            modifier = Modifier.clickable {
                                // TODO: 点击后执行操作
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}