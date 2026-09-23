package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.NotificationFeaturePreferences
import com.example.data.OtpCatcherPreferences
import com.example.service.NotificationReliability
import com.example.ui.components.AppPickerMode
import com.example.ui.components.AppPickerSheet
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonPalette
import com.example.ui.theme.EmeraldPalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapeM
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeaturePackSettings(viewModelFeaturePreferences: NotificationFeaturePreferences) {
    val context = LocalContext.current
    val colors = LocalVaultColors.current
    val otp = remember { OtpCatcherPreferences.getInstance(context) }
    val enabled by otp.otpEnabled.collectAsStateWithLifecycle()
    val emoji by otp.useEmojiDigits.collectAsStateWithLifecycle()
    val copyWithApp by otp.copyWithAppName.collectAsStateWithLifecycle()
    val timeout by otp.timeoutMs.collectAsStateWithLifecycle()
    val sound by otp.soundMode.collectAsStateWithLifecycle()
    val soundUri by otp.soundUri.collectAsStateWithLifecycle()
    val vibration by otp.vibrationMode.collectAsStateWithLifecycle()
    val lockscreen by otp.lockscreenMode.collectAsStateWithLifecycle()
    val catchExcluded by otp.catchExcluded.collectAsStateWithLifecycle()
    val selectedApps by otp.selectedPackages.collectAsStateWithLifecycle()
    val excludedApps by otp.excludedPackages.collectAsStateWithLifecycle()
    val otpLog by otp.otpLog.collectAsStateWithLifecycle()
    val separateUpdates by viewModelFeaturePreferences.showUpdatesSeparately.collectAsState()
    var picker by remember { mutableStateOf<String?>(null) }
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { uri ->
            otp.setSoundUri(uri.toString())
            otp.setSoundMode("custom")
            com.example.service.OtpNotifier(context).recreateChannel()
        }
    }

    if (picker != null) {
        AppPickerSheet(
            title = if (picker == "selected") "OTP source apps" else "OTP excluded apps",
            mode = if (picker == "selected") AppPickerMode.MULTI_SELECT_CUSTOM_CHIP else AppPickerMode.MULTI_SELECT_EXCLUDE,
            initiallySelected = if (picker == "selected") selectedApps else excludedApps,
            onClose = { picker = null },
            onConfirmSelection = {
                if (picker == "selected") otp.setSelectedPackages(it) else otp.setExcludedPackages(it)
                picker = null
            }
        )
        return
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("SMART CAPTURE", style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary))
        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxWidth(), shape = ShapeL, accentBorder = true) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Duplicate Suppression", style = VaultTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Text("One fingerprint per live notification. Changed content is kept as history instead of creating another row.", style = VaultCaption.copy(color = colors.textTertiary))
                SettingSwitchRow(
                    title = "Show updates as separate entries",
                    subtitle = "Keep changed versions as new archive cards",
                    checked = separateUpdates,
                    onCheckedChange = viewModelFeaturePreferences::setShowUpdatesSeparately
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("OTP CATCHER", style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary))
        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxWidth(), shape = ShapeL, accentBorder = enabled) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, null, tint = colors.accent.base, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Always-on OTP Catcher", style = VaultTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            Text("Private alert with one-tap copy", style = VaultCaption.copy(color = colors.textTertiary))
                        }
                    }
                    Switch(checked = enabled, onCheckedChange = otp::setOtpEnabled, colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow))
                }
                SettingSwitchRow("Emoji digits", "Show keycap digits in the alert", emoji, otp::setUseEmojiDigits)
                SettingSwitchRow("Copy with app name", "Copy “App: 123456” instead of only the code", copyWithApp, otp::setCopyWithAppName)
                SettingSwitchRow("Catch from excluded apps", "Allow OTPs even when capture rules mute the app", catchExcluded, otp::setCatchExcluded)
                SelectRow("Auto-dismiss", timeoutLabel(timeout), listOf(30_000L to "30 seconds", 60_000L to "60 seconds", 120_000L to "2 minutes", -1L to "Never")) { otp.setTimeout(it) }
                SelectRow("Sound", soundLabel(sound, soundUri), listOf("off" to "Off", "default" to "Default", "custom" to "Custom tone")) {
                    if (it == "custom") {
                        ringtoneLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "OTP Catcher tone")
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    soundUri.takeIf { value -> value.isNotBlank() }?.let(android.net.Uri::parse)
                                )
                            }
                        )
                    } else {
                        otp.setSoundMode(it)
                        com.example.service.OtpNotifier(context).recreateChannel()
                    }
                }
                SelectRow("Vibration", vibrationLabel(vibration), listOf("off" to "Off", "short" to "Short", "long" to "Long")) {
                    otp.setVibrationMode(it)
                    com.example.service.OtpNotifier(context).recreateChannel()
                }
                SelectRow("Lockscreen", if (lockscreen == "hide") "Hide code" else "Show privately", listOf("hide" to "Hide code", "show" to "Show privately")) {
                    otp.setLockscreenMode(it)
                    com.example.service.OtpNotifier(context).recreateChannel()
                }
                PickerRow("Include OTPs from", if (selectedApps.isEmpty()) "All apps" else "${selectedApps.size} selected apps") { picker = "selected" }
                PickerRow("Exclude OTPs from", if (excludedApps.isEmpty()) "None" else "${excludedApps.size} excluded apps") { picker = "excluded" }

                if (otpLog.isNotEmpty()) {
                    Text("LAST 20 CAUGHT", style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary))
                    otpLog.forEach { log ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(log.sourceApp, style = VaultBodyM.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = colors.textPrimary)
                                Text(log.maskedCode, style = VaultCaption.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = colors.accent.base))
                            }
                            Text(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)), style = VaultCaption.copy(color = colors.textTertiary))
                        }
                    }
                } else {
                    Text("OTP history is empty. New caught codes are stored locally in the vault.", style = VaultCaption.copy(color = colors.textTertiary))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        BatteryOptimizationCard(context)
    }
}

@Composable
fun BatteryOptimizationOnboardingPrompt(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("notifyvault_reliability", Context.MODE_PRIVATE) }
    var visible by remember {
        mutableStateOf(
            !prefs.getBoolean("battery_prompt_shown", false) &&
                !NotificationReliability.isProtected(context)
        )
    }
    if (!visible) return
    AlertDialog(
        onDismissRequest = {
            prefs.edit().putBoolean("battery_prompt_shown", true).apply()
            visible = false
            onDismiss()
        },
        title = { Text("Keep NotifyVault active", style = VaultTitle.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Text(
                "Android may pause notification listeners during Doze or battery restriction. Allowing unrestricted battery use keeps deduplication and OTP delivery available after long idle periods.",
                style = VaultBodyM.copy(fontSize = 13.sp)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                prefs.edit().putBoolean("battery_prompt_shown", true).apply()
                NotificationReliability.openBatteryOptimizationRequest(context)
                visible = false
                onDismiss()
            }) {
                Text("Allow", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                prefs.edit().putBoolean("battery_prompt_shown", true).apply()
                NotificationReliability.openAutoStartSettings(context)
                visible = false
                onDismiss()
            }) {
                Text("OEM autostart")
            }
        }
    )
}

@Composable
private fun BatteryOptimizationCard(context: Context) {
    val colors = LocalVaultColors.current
    val exempt = remember { mutableStateOf(NotificationReliability.isProtected(context)) }
    GlassCard(Modifier.fillMaxWidth(), shape = ShapeL, accentBorder = !exempt.value) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryChargingFull, null, tint = if (exempt.value) EmeraldPalette.base else CrimsonPalette.base, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(if (exempt.value) "Background reliability protected" else "Protect background delivery", style = VaultTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Text("Battery restrictions can stop notification listeners and OTP delivery.", style = VaultCaption.copy(color = colors.textTertiary))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (exempt.value) "Battery unrestricted" else "Allow",
                    style = VaultCaption.copy(color = if (exempt.value) EmeraldPalette.base else colors.accent.base, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clip(ShapePill).background(colors.elevated).clickable {
                        NotificationReliability.openBatteryOptimizationRequest(context)
                        exempt.value = NotificationReliability.isProtected(context)
                    }.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    "OEM autostart",
                    style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clip(ShapePill).background(colors.elevated).clickable {
                        NotificationReliability.openAutoStartSettings(context)
                    }.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                if (NotificationReliability.isBackgroundRestricted(context)) {
                    Text(
                        "App battery state is Restricted",
                        style = VaultCaption.copy(color = CrimsonPalette.base, fontSize = 10.sp),
                        modifier = Modifier
                            .clip(ShapePill)
                            .clickable { NotificationReliability.openAppBatterySettings(context) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalVaultColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = VaultBodyM.copy(fontSize = 13.sp), color = colors.textPrimary)
            Text(subtitle, style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow))
    }
}

@Composable
private fun PickerRow(title: String, value: String, onClick: () -> Unit) {
    val colors = LocalVaultColors.current
    Row(Modifier.fillMaxWidth().clip(ShapeM).background(colors.elevated).clickable(onClick = onClick).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = VaultBodyM.copy(fontSize = 13.sp), color = colors.textPrimary)
        Text(value, style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun <T> SelectRow(title: String, value: String, options: List<Pair<T, String>>, onSelected: (T) -> Unit) {
    val colors = LocalVaultColors.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clip(ShapeM).background(colors.elevated).clickable { expanded = true }.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = VaultBodyM.copy(fontSize = 13.sp), color = colors.textPrimary)
            Text(value, style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSelected(key); expanded = false })
            }
        }
    }
}

private fun timeoutLabel(timeout: Long) = when (timeout) {
    30_000L -> "30 seconds"
    120_000L -> "2 minutes"
    -1L -> "Never"
    else -> "60 seconds"
}
private fun soundLabel(value: String, uri: String) = when {
    value == "off" -> "Off"
    value == "custom" && uri.isNotBlank() -> "Custom tone"
    else -> "Default"
}
private fun vibrationLabel(value: String) = when (value) {
    "off" -> "Off"
    "long" -> "Long"
    else -> "Short"
}