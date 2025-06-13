package me.neko.nzhelper.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import android.content.Intent
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import me.neko.nzhelper.BuildConfig
import androidx.core.net.toUri
import me.neko.nzhelper.ui.util.BottomNavItem
import me.neko.nzhelper.ui.util.UpdateChecker

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar {
        BottomNavItem.Companion.items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val owner = "bug-bit"
    val repo  = "NzHelper"

    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestTag by remember { mutableStateOf<String?>(null) }

    fun stripSuffix(version: String): String =
        version.trimStart('v','V').substringBefore('-')

    fun parseNumbers(version: String): List<Int> =
        stripSuffix(version)
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
            .let {
                when {
                    it.size >= 3 -> it.take(3)
                    it.size == 2 -> it + listOf(0)
                    it.size == 1 -> it + listOf(0, 0)
                    else -> listOf(0, 0, 0)
                }
            }

    fun isRemoteGreater(local: String, remote: String): Boolean {
        val localNums  = parseNumbers(local)
        val remoteNums = parseNumbers(remote)
        for (i in 0..2) {
            if (remoteNums[i] > localNums[i]) return true
            if (remoteNums[i] < localNums[i]) return false
        }
        return false
    }

    LaunchedEffect(Unit) {
        UpdateChecker.fetchLatestVersion(owner, repo)?.let { remoteVer ->
            latestTag = remoteVer
            if (isRemoteGreater(BuildConfig.VERSION_NAME, remoteVer)) {
                showUpdateDialog = true
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { HomeScreen() }
            composable(BottomNavItem.History.route) { HistoryScreen() }
            composable(BottomNavItem.Settings.route) { SettingsScreen() }
        }

        if (showUpdateDialog && latestTag != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("检测到新版本") },
                text = {
                    Text(
                        "当前版本：${BuildConfig.VERSION_NAME}\n" +
                                "最新版本：$latestTag\n\n" +
                                "针对你的牛牛进行了一些优化，是否前往 GitHub 下载？"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showUpdateDialog = false
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/$owner/$repo/releases/latest".toUri()
                        )
                        context.startActivity(intent)
                    }) { Text("去下载") }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("稍后再说")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}
