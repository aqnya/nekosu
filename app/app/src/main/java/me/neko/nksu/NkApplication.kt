package me.neko.nksu

import android.app.Application
import me.neko.nksu.ui.util.CrashHandler
import me.neko.nksu.ui.util.NotificationUtil

class NkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 在应用启动时创建通知渠道
        NotificationUtil.createChannel(this)
        CrashHandler.init(this)
    }
}