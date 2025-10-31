package me.neko.nzhelper.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import me.neko.nzhelper.BuildConfig
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "NzHelper",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 状态卡片 (MD3 风格)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        // TODO: 导航到安装页面
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "未安装",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "点击安装辅助服务",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.ArrowForwardIos,
                        contentDescription = "前往安装",
                        tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 设备信息卡片 - 优化版本
            DeviceInfoCard(
                modifier = Modifier.fillMaxWidth(),
                onInfoCopy = { info ->
                    clipboardManager.setText(AnnotatedString(info))
                    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                }
            )

            // 快速操作卡片
            QuickActionsCard(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DeviceInfoCard(
    modifier: Modifier = Modifier,
    onInfoCopy: (String) -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题栏
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "设备信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "点击项目复制",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // 设备信息网格
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 第一行：基本设备信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DeviceInfoItem(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "设备型号",
                        value = Build.MODEL,
                        modifier = Modifier.weight(1f),
                        onCopy = onInfoCopy
                    )
                    DeviceInfoItem(
                        icon = Icons.Filled.Build,
                        title = "制造商",
                        value = Build.MANUFACTURER,
                        modifier = Modifier.weight(1f),
                        onCopy = onInfoCopy
                    )
                }

                // 第二行：系统版本信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DeviceInfoItem(
                        icon = Icons.Filled.Android,
                        title = "Android 版本",
                        value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        modifier = Modifier.weight(1f),
                        onCopy = onInfoCopy
                    )
                    DeviceInfoItem(
                        icon = Icons.Filled.Security,
                        title = "安全补丁",
                        value = Build.VERSION.SECURITY_PATCH,
                        modifier = Modifier.weight(1f),
                        onCopy = onInfoCopy
                    )
                }

                // 第三行：硬件信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DeviceInfoItem(
                        icon = Icons.Filled.DeviceHub,
                        title = "硬件平台",
                        value = Build.HARDWARE,
                        modifier = Modifier.weight(1f),
                        onCopy = onInfoCopy
                    )
                    DeviceInfoItem(
                        icon = Icons.Filled.Memory,
                        title = "CPU 架构",
                        value = Build.SUPPORTED_ABIS.firstOrNull() ?: "未知",
                        modifier = Modifier.weight(1f),
                        onCopy = onInfoCopy
                    )
                }

                // 第四行：应用信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DeviceInfoItem(
                        icon = Icons.Outlined.Info,
                        title = "应用版本",
                        value = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        modifier = Modifier.weight(1f),
                        onCopy = onInfoCopy
                    )
                    DeviceInfoItem(
                        icon = Icons.Filled.Apps,
                        title = "Android 版本",
                        value = "SDK ${Build.VERSION.SDK_INT}",
                        modifier = Modifier.weight(1f),
                        onCopy = onInfoCopy
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onCopy: (String) -> Unit = {}
) {
    Card(
        modifier = modifier
            .clickable { onCopy("$title: $value") },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun QuickActionsCard(
    modifier: Modifier = Modifier
) {
    val quickActions = listOf(
        QuickAction(
            icon = Icons.Filled.Settings,
            title = "系统设置",
            description = "打开系统设置"
        ),
        QuickAction(
            icon = Icons.Filled.DeveloperMode,
            title = "开发者选项",
            description = "打开开发者选项"
        ),
        QuickAction(
            icon = Icons.Filled.Apps,
            title = "应用列表",
            description = "查看所有应用"
        ),
        QuickAction(
            icon = Icons.Filled.Storage,
            title = "存储信息",
            description = "查看存储空间"
        )
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "快速操作",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickActions) { action ->
                    QuickActionItem(
                        action = action,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(
    action: QuickAction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { /* TODO: 实现对应操作 */ },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "执行操作",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

data class QuickAction(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceInfoCardPreview() {
    MaterialTheme {
        DeviceInfoCard(
            modifier = Modifier.padding(16.dp)
        )
    }
}