package me.neko.nzhelper.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm: PackageManager = context.packageManager
            val installed = pm.getInstalledPackages(PackageManager.GET_META_DATA)
    .sortedBy { it.applicationInfo.loadLabel(pm).toString().lowercase() }
    .map { pkgInfo ->
        AppInfo(
            name = pkgInfo.applicationInfo.loadLabel(pm).toString(),
            packageName = pkgInfo.packageName,
            icon = pkgInfo.applicationInfo.loadIcon(pm).toBitmap().asImageBitmap()
        )
    }
            apps = installed
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已安装应用") }
            )
        }
    ) { innerPadding ->
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(apps) { app ->
                    ListItem(
                        headlineContent = {
                            Text(app.name)
                        },
                        supportingContent = {
                            Text(app.packageName)
                        },
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
                    Divider() // 分隔线，让列表更规整
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