package com.example.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ResolvedAppInfo(
    val packageName: String,
    val appName: String,
    val iconPath: String?
)

object AppInfoResolver {

    private val memoryCache = LruCache<String, ResolvedAppInfo>(200)

    // Known package name fallbacks if uninstalled or not found by PackageManager
    private val KNOWN_APPS = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.google.android.gm" to "Gmail",
        "com.google.android.apps.messaging" to "Messages",
        "com.android.mms" to "Messages",
        "com.facebook.katana" to "Facebook",
        "com.facebook.orca" to "Messenger",
        "com.instagram.android" to "Instagram",
        "com.twitter.android" to "X (Twitter)",
        "org.telegram.messenger" to "Telegram",
        "com.spotify.music" to "Spotify",
        "com.slack" to "Slack",
        "com.discord" to "Discord",
        "com.google.android.youtube" to "YouTube",
        "com.uber.uberx" to "Uber",
        "com.amazon.mShop.android.shopping" to "Amazon",
        "com.flipkart.android" to "Flipkart",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.snapchat.android" to "Snapchat",
        "com.netflix.mediaclient" to "Netflix",
        "com.microsoft.teams" to "Microsoft Teams",
        "com.microsoft.office.outlook" to "Outlook"
    )

    fun cleanPackageName(packageName: String): String {
        KNOWN_APPS[packageName]?.let { return it }
        val parts = packageName.split(".")
        val candidate = parts.lastOrNull { it !in listOf("android", "app", "mobile", "client", "ui", "internal") }
            ?: parts.lastOrNull() ?: packageName
        return candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    suspend fun resolve(
        context: Context,
        packageName: String,
        database: VaultDatabase? = null
    ): ResolvedAppInfo = withContext(Dispatchers.IO) {
        // 1. Check memory cache
        memoryCache.get(packageName)?.let { return@withContext it }

        // 2. Check Room database cache
        val db = database ?: VaultDatabase.getInstance(context)
        val cachedEntity = try {
            db.appInfoDao().getAppInfo(packageName)
        } catch (e: Exception) {
            null
        }

        if (cachedEntity != null) {
            val resolved = ResolvedAppInfo(
                packageName = cachedEntity.packageName,
                appName = cachedEntity.appName,
                iconPath = cachedEntity.iconPath
            )
            memoryCache.put(packageName, resolved)
            return@withContext resolved
        }

        // 3. Query PackageManager
        val resolved = resolveFromPackageManager(context, packageName, db)
        memoryCache.put(packageName, resolved)
        resolved
    }

    fun resolveSync(
        context: Context,
        packageName: String,
        database: VaultDatabase? = null
    ): ResolvedAppInfo {
        memoryCache.get(packageName)?.let { return it }

        val db = database ?: VaultDatabase.getInstance(context)
        val resolved = resolveFromPackageManager(context, packageName, db)
        memoryCache.put(packageName, resolved)
        return resolved
    }

    private fun resolveFromPackageManager(
        context: Context,
        packageName: String,
        db: VaultDatabase
    ): ResolvedAppInfo {
        val pm = context.packageManager
        var appLabel: String? = null
        var iconPath: String? = null

        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appLabel = pm.getApplicationLabel(appInfo).toString()
            val drawable = pm.getApplicationIcon(appInfo)
            iconPath = saveDrawableToFile(context, packageName, drawable)
        } catch (e: Exception) {
            // App might be uninstalled or hidden
            appLabel = cleanPackageName(packageName)
        }

        val finalName = if (!appLabel.isNullOrBlank() && appLabel != packageName) {
            appLabel
        } else {
            cleanPackageName(packageName)
        }

        val entity = AppInfoEntity(
            packageName = packageName,
            appName = finalName,
            iconPath = iconPath
        )

        try {
            // Asynchronous, completely non-blocking database insertion
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.appInfoDao().insert(entity)
                } catch (e: Exception) {
                    // Ignored
                }
            }
        } catch (e: Exception) {
            // Fallback gracefully
        }

        return ResolvedAppInfo(packageName, finalName, iconPath)
    }

    fun saveDrawableToFile(context: Context, key: String, drawable: Drawable): String? {
        return try {
            val iconsDir = File(context.filesDir, "app_icons").apply { if (!exists()) mkdirs() }
            val cleanKey = key.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val iconFile = File(iconsDir, "${cleanKey}.png")

            val bitmap = drawableToBitmap(drawable)
            FileOutputStream(iconFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            iconFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun saveBitmapToFile(context: Context, subDir: String, key: String, bitmap: Bitmap): String? {
        return try {
            val targetDir = File(context.filesDir, subDir).apply { if (!exists()) mkdirs() }
            val cleanKey = key.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(targetDir, "${cleanKey}.png")

            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
