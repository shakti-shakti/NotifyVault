package com.example.service

import com.example.data.NotificationEntity

data class OtpMatch(
    val code: String,
    val sourceApp: String,
    val sourcePackage: String,
    val matchedKeyword: String
)

object OtpDetector {
    private val codeRegex = Regex("(?<!\\d)(\\d{4}|\\d{6}|\\d{8})(?!\\d)")
    private val keywordRegex = Regex(
        "\\b(otp|one[- ]?time password|verification code|verify code|security code|auth(?:entication)? code|login code|sign[- ]in code|confirmation code|passcode|pin|code)\\b",
        RegexOption.IGNORE_CASE
    )
    private val inherentlyOtpPackages = setOf(
        "com.google.android.gms",
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.walletnfcrel",
        "in.org.npci.upiapp"
    )

    fun detect(entity: NotificationEntity): OtpMatch? {
        val content = listOf(
            entity.title, entity.text, entity.bigText, entity.subText,
            entity.summaryText, entity.infoText
        ).filterNotNull().joinToString(" ").trim()
        if (content.isBlank()) return null
        val packageIsWhitelisted = entity.packageName in inherentlyOtpPackages ||
            listOf("bank", "pay", "upi", "wallet").any { entity.packageName.contains(it, true) }
        val keyword = keywordRegex.find(content)
        if (keyword == null && !packageIsWhitelisted) return null

        val candidates = codeRegex.findAll(content)
            .filter { match ->
                val before = content.substring(0, match.range.first)
                val after = content.substring(match.range.last + 1)
                val urlPrefix = before.substringAfterLast(' ', before).lowercase()
                !urlPrefix.contains("http://") &&
                    !urlPrefix.contains("https://") &&
                    !after.takeWhile { !it.isWhitespace() }.contains("/")
            }
            .map { it.groupValues[1] }
            .toList()
        val code = candidates.firstOrNull { it.length == 6 }
            ?: candidates.firstOrNull { it.length == 8 }
            ?: candidates.firstOrNull { it.length == 4 && keyword != null }
            ?: return null
        return OtpMatch(
            code = code,
            sourceApp = entity.appName,
            sourcePackage = entity.packageName,
            matchedKeyword = keyword?.value ?: "trusted package"
        )
    }
}