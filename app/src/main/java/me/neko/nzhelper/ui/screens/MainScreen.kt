package me.neko.nzhelper.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val (hasPermission, setHasPermission) = remember {
        mutableStateOf(checkStoragePermission())
    }

    LaunchedEffect(Unit) {
        // 每次进入时重新检查权限状态
        setHasPermission(checkStoragePermission())
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        if (hasPermission) {
            // ✅ 已获得权限，进入主界面
            FileBrowserScreen()
        } else {
            // ❌ 未获得权限，请求
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("The app needs storage permission to function properly.")
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    } else {
                        // Android 10 及以下走普通权限
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    }
                }) {
                    Text("Request Permission")
                }
            }
        }
    }
}

@Composable
private fun checkStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        // 对旧版本使用 READ/WRITE_EXTERNAL_STORAGE 检查
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}
