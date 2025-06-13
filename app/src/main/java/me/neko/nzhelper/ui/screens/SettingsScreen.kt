package me.neko.nzhelper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = icon,
        headlineContent = { Text(text = title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = summary
            ?.let { { Text(text = it, style = MaterialTheme.typography.bodyMedium) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") }, windowInsets = WindowInsets(0, 0, 0, 0))
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "常规",
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            PreferenceItem(
                title = "关于",
                summary = null,
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                onClick = { showAboutDialog = true }
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    // 动态读取版本号
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
    }

    val uriHandler = LocalUriHandler.current
    val url = "https://github.com/zzzdajb/DickHelper"
    val label = "DickHelper"

    val annotated = buildAnnotatedString {
        append("这是一个以开源项目 ")

        val start = length
        append(label)
        val end = length

        addStyle(
            style = SpanStyle(textDecoration = TextDecoration.Underline),
            start = start,
            end = end
        )
        addLink(
            LinkAnnotation.Clickable(
                tag = label,
                linkInteractionListener = { uriHandler.openUri(url) }
            ),
            start = start,
            end = end
        )

        append(" 作为参考的使用 Kotlin 编写的 App")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于本应用") },
        text = {
            Column {
                Text("版本：$versionName")
                Spacer(Modifier.height(8.dp))
                Text(text = annotated, style = TextStyle())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = true)
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
