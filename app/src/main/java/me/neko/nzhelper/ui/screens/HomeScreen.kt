package me.neko.nzhelper.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.BuildConfig

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        // 设备信息卡片
        InfoCard(
    title = "设备信息",
    content = listOf(
        "设备型号: ${Build.MODEL}",
        "制造商: ${Build.MANUFACTURER}",
        "Android 版本: ${Build.VERSION.RELEASE}",
        "SDK: ${Build.VERSION.SDK_INT}",
        "设备: ${Build.DEVICE}",
        "品牌: ${Build.BRAND}"
    ),
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
)

Spacer(modifier = Modifier.height(16.dp))

InfoCard(
    title = "应用信息",
    content = listOf(
        "应用名称: ${context.applicationInfo.loadLabel(context.packageManager)}",
        "版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        "包名: ${context.packageName}"
    ),
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer
)
    }
}

@Composable
fun InfoCard(
    title: String,
    content: List<String>,
    containerColor: androidx.compose.ui.graphics.Color,
    onContainerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // 去除阴影
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = onContainerColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            content.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainerColor
                )
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}