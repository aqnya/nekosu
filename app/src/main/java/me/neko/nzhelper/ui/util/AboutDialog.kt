package me.neko.nzhelper.ui.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import me.neko.nzhelper.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
    }
    val uriHandler = LocalUriHandler.current
    val url = "https://github.com/zzzdajb/DickHelper"
    val label = "DickHelper"

    val sourceLabel = "GitHub"
    val sourceUrl = "https://github.com/bug-bit/NzHelper"

    val showDonateDialog = remember { mutableStateOf(false) }

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
    val viewSource = buildAnnotatedString {
        append("在 ")
        val start = length
        append(sourceLabel)
        val end = length
        addStyle(
            style = SpanStyle(textDecoration = TextDecoration.Underline),
            start = start,
            end = end
        )
        addLink(
            LinkAnnotation.Clickable(
                tag = sourceLabel,
                linkInteractionListener = { uriHandler.openUri(sourceUrl) }
            ),
            start = start,
            end = end
        )
        append(" 查看源码")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于本应用") },
        text = {
            Column {
                Text("版本：$versionName")
                Spacer(Modifier.height(8.dp))
                Text(text = annotated, style = TextStyle())
                Spacer(Modifier.height(8.dp))
                Text(text = viewSource, style = TextStyle())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDonateDialog.value = true }) {
                Text("捐赠打赏")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = true)
    )

    if (showDonateDialog.value) {
        AlertDialog(
            onDismissRequest = { showDonateDialog.value = false },
            title = { Text("打赏我们") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("祝愿所有给我们打赏的小伙伴牛子长度翻倍 ❤️ 您的捐赠将是我们更新的动力")
                    Spacer(Modifier.height(16.dp))
                    Image(
                        painter = painterResource(id = R.drawable.weixin), //你的二维码资源
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDonateDialog.value = false }) {
                    Text("知道了")
                }
            }
        )
    }
}
