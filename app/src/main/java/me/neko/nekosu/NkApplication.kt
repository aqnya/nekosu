package me.neko.Nekosu

import android.app.Application
import me.neko.Nekosu.ui.util.NotificationUtil

class NzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 在应用启动时创建通知渠道
        NotificationUtil.createChannel(this)
    }
}