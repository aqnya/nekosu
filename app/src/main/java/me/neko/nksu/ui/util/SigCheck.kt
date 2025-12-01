package me.neko.nksu.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.security.MessageDigest

/**
 * 签名检查工具类
 * 用于校验应用签名的完整性，防止被篡改
 */
object SigCheck {

    // TODO: 请将此处替换为你的发布版 Keystore 的 SHA-256 值 (大写，无冒号)
    // 可以通过 ./gradlew signingReport 或 keytool 命令获取
    private const val EXPECTED_SIGNATURE = "A26EA8F25044AFA79B11862F23729A4EF97F25CF63D37C0E74484365FFB5ED25"
    
    private const val TAG = "SigCheck"

    /**
     * 校验当前应用的签名是否与预期的签名一致
     * 兼容 Android P (API 28) 以上的 V2/V3 签名方案及密钥轮换
     */
    fun validate(context: Context): Boolean {
        try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+ 使用 GET_SIGNING_CERTIFICATES 以支持 V2/V3 签名和密钥轮换
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                // 旧版 API 使用 GET_SIGNATURES
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return false

            // 遍历所有找到的签名（通常只有一个，除非涉及密钥轮换历史）
            for (signature in signatures) {
                val currentSignature = getSHA256(signature.toByteArray())
                
                // 开发调试时打印当前签名，发布时建议移除或保留为 debug 日志
                Log.d(TAG, "检测到应用签名: $currentSignature")

                if (EXPECTED_SIGNATURE == currentSignature) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "签名校验过程发生错误", e)
        }
        return false
    }

    /**
     * 计算字节数组的 SHA-256 哈希值
     */
    private fun getSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02X".format(it) }
    }
}

