package me.neko.nksu

import android.app.Application
import me.neko.nksu.ui.util.CrashHandler
import me.neko.nksu.ui.util.NotificationUtil

class NkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createChannel(this)
        CrashHandler.init(this)
    }
}