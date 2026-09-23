package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.BiometricAuthHelper
import com.example.security.LockManager
import com.example.security.LockMethod
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.LockKeypad
import com.example.ui.components.PatternLockView
import com.example.ui.theme.CrimsonPalette
import com.example.ui.theme.DisplayM
import com.example.ui.theme.EmeraldPalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapeM
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle

enum class SetupStep {
    CHOOSE_METHOD,
    ENTER_CREDENTIAL,
    CONFIRM_CREDENTIAL,
    SUCCESS
}

@Composable
fun LockSetupScreen(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    val lockManager = remember { LockManager.getInstance(context) }
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    var step by remember { mutableStateOf(SetupStep.CHOOSE_METHOD) }
    var selectedMethod by remember { mutableStateOf(LockMethod.PIN) }

    // PIN state
    var enteredPin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }

    // Password state
    var enteredPassword by remember { mutableStateOf("") }
    var confirmedPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Pattern state
    var enteredPattern by remember { mutableStateOf("") }
    var confirmedPattern by remember { mutableStateOf("") }

    // Biometric option
    var biometricEnabled by remember { mutableStateOf(BiometricAuthHelper.isBiometricAvailable(context)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerHapticError() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80L)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Shield Icon & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.accent.glow)
                        .border(1.5.dp, colors.accent.base, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = colors.accent.base,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (step) {
                        SetupStep.CHOOSE_METHOD -> "Secure Your Vault"
                        SetupStep.ENTER_CREDENTIAL -> "Set Your ${selectedMethod.title}"
                        SetupStep.CONFIRM_CREDENTIAL -> "Confirm Your ${selectedMethod.title}"
                        SetupStep.SUCCESS -> "Vault Secured"
                    },
                    style = DisplayM.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (step) {
                        SetupStep.CHOOSE_METHOD -> "Choose your primary authentication lock"
                        SetupStep.ENTER_CREDENTIAL -> when (selectedMethod) {
                            LockMethod.PIN -> "Enter a 4 to 6-digit security PIN"
                            LockMethod.PASSWORD -> "Create a password (minimum 4 characters)"
                            LockMethod.PATTERN -> "Connect at least 4 dots to form a pattern"
                            else -> ""
                        }
                        SetupStep.CONFIRM_CREDENTIAL -> "Re-enter the exact same credential to confirm"
                        SetupStep.SUCCESS -> "Your notification archive is encrypted at rest"
                    },
                    style = VaultCaption.copy(fontSize = 12.sp, color = colors.textTertiary)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = VaultCaption.copy(color = CrimsonPalette.base, fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // Center content based on step
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    SetupStep.CHOOSE_METHOD -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                LockMethod.PIN to ("4–6 Digit PIN" to "Fast, numeric keypad unlock"),
                                LockMethod.PASSWORD to ("Alphanumeric Password" to "Complex protection with symbols & numbers"),
                                LockMethod.PATTERN to ("3x3 Pattern Lock" to "Fluid gesture trail connection")
                            ).forEach { (method, desc) ->
                                val isSelected = selectedMethod == method
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ShapeL,
                                    accentBorder = isSelected,
                                    onClick = { selectedMethod = method }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) colors.accent.glow else colors.surface)
                                                    .border(1.dp, if (isSelected) colors.accent.base else colors.cardStroke, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (method) {
                                                        LockMethod.PIN -> Icons.Default.Pin
                                                        LockMethod.PASSWORD -> Icons.Default.Password
                                                        LockMethod.PATTERN -> Icons.Default.Pattern
                                                        else -> Icons.Default.Lock
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isSelected) colors.accent.base else colors.textSecondary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column {
                                                Text(
                                                    text = desc.first,
                                                    style = VaultTitle.copy(
                                                        fontSize = 15.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = desc.second,
                                                    style = VaultCaption.copy(fontSize = 11.sp, color = colors.textTertiary)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) colors.accent.base else Color.Transparent)
                                                .border(1.5.dp, if (isSelected) colors.accent.base else colors.cardStroke, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, null, tint = Color(0xFF0A0A10), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            if (BiometricAuthHelper.isBiometricAvailable(context)) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ShapeM
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = null,
                                                tint = colors.accent.base,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Fast Biometric Unlock",
                                                    style = VaultBodyM.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = "Uses fingerprint or face on top of lock",
                                                    style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary)
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = biometricEnabled,
                                            onCheckedChange = { biometricEnabled = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = colors.accent.base,
                                                checkedTrackColor = colors.accent.glow
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SetupStep.ENTER_CREDENTIAL -> {
                        when (selectedMethod) {
                            LockMethod.PIN -> {
                                LockKeypad(
                                    pinLength = 6,
                                    currentLength = enteredPin.length,
                                    onDigitClick = { digit ->
                                        if (enteredPin.length < 6) {
                                            enteredPin += digit
                                            errorMessage = null
                                        }
                                    },
                                    onDeleteClick = {
                                        if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                    }
                                )
                            }
                            LockMethod.PASSWORD -> {
                                val strength = lockManager.evaluatePasswordStrength(enteredPassword)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(ShapePill)
                                            .background(colors.surface)
                                            .border(1.dp, colors.cardStroke, ShapePill)
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BasicTextField(
                                                value = enteredPassword,
                                                onValueChange = {
                                                    enteredPassword = it
                                                    errorMessage = null
                                                },
                                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                                textStyle = TextStyle(
                                                    color = colors.textPrimary,
                                                    fontSize = 16.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                cursorBrush = SolidColor(colors.accent.base),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                decorationBox = { inner ->
                                                    if (enteredPassword.isEmpty()) {
                                                        Text("Enter new password…", style = VaultBodyM.copy(color = colors.textTertiary))
                                                    }
                                                    inner()
                                                }
                                            )
                                            Icon(
                                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = colors.textTertiary,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable { showPassword = !showPassword }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Strength bar
                                    val barColor = when (strength.score) {
                                        1 -> CrimsonPalette.base
                                        2 -> Color(0xFFF97316)
                                        3 -> Color(0xFFEAB308)
                                        4 -> EmeraldPalette.base
                                        else -> colors.cardStroke
                                    }
                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Password Strength", style = VaultCaption.copy(fontSize = 11.sp, color = colors.textTertiary))
                                            Text(strength.label, style = VaultCaption.copy(fontSize = 11.sp, color = barColor, fontWeight = FontWeight.Bold))
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            for (i in 1..4) {
                                                val filled = i <= strength.score
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(4.dp)
                                                        .clip(ShapePill)
                                                        .background(if (filled) barColor else colors.surface)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(strength.feedback, style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary))
                                    }
                                }
                            }
                            LockMethod.PATTERN -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    PatternLockView(
                                        onPatternComplete = { pattern ->
                                            val dots = pattern.split("-")
                                            if (dots.size < 4) {
                                                errorMessage = "Connect at least 4 dots"
                                                triggerHapticError()
                                            } else {
                                                enteredPattern = pattern
                                                errorMessage = null
                                                step = SetupStep.CONFIRM_CREDENTIAL
                                            }
                                        }
                                    )
                                }
                            }
                            else -> {}
                        }
                    }

                    SetupStep.CONFIRM_CREDENTIAL -> {
                        when (selectedMethod) {
                            LockMethod.PIN -> {
                                LockKeypad(
                                    pinLength = enteredPin.length.coerceIn(4, 6),
                                    currentLength = confirmedPin.length,
                                    onDigitClick = { digit ->
                                        if (confirmedPin.length < enteredPin.length) {
                                            confirmedPin += digit
                                            errorMessage = null
                                            if (confirmedPin.length == enteredPin.length) {
                                                if (confirmedPin == enteredPin) {
                                                    // Save credential
                                                    lockManager.setCredential(LockMethod.PIN, enteredPin)
                                                    lockManager.setBiometricEnabled(biometricEnabled)
                                                    step = SetupStep.SUCCESS
                                                } else {
                                                    errorMessage = "PINs do not match. Try again."
                                                    triggerHapticError()
                                                    confirmedPin = ""
                                                }
                                            }
                                        }
                                    },
                                    onDeleteClick = {
                                        if (confirmedPin.isNotEmpty()) confirmedPin = confirmedPin.dropLast(1)
                                    }
                                )
                            }
                            LockMethod.PASSWORD -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(ShapePill)
                                            .background(colors.surface)
                                            .border(1.dp, colors.cardStroke, ShapePill)
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        BasicTextField(
                                            value = confirmedPassword,
                                            onValueChange = {
                                                confirmedPassword = it
                                                errorMessage = null
                                            },
                                            visualTransformation = PasswordVisualTransformation(),
                                            textStyle = TextStyle(
                                                color = colors.textPrimary,
                                                fontSize = 16.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            cursorBrush = SolidColor(colors.accent.base),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            decorationBox = { inner ->
                                                if (confirmedPassword.isEmpty()) {
                                                    Text("Re-type password…", style = VaultBodyM.copy(color = colors.textTertiary))
                                                }
                                                inner()
                                            }
                                        )
                                    }
                                }
                            }
                            LockMethod.PATTERN -> {
                                PatternLockView(
                                    onPatternComplete = { pattern ->
                                        if (pattern == enteredPattern) {
                                            lockManager.setCredential(LockMethod.PATTERN, pattern)
                                            lockManager.setBiometricEnabled(biometricEnabled)
                                            step = SetupStep.SUCCESS
                                        } else {
                                            errorMessage = "Patterns do not match. Try again."
                                            triggerHapticError()
                                        }
                                    }
                                )
                            }
                            else -> {}
                        }
                    }

                    SetupStep.SUCCESS -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPalette.glow)
                                    .border(2.dp, EmeraldPalette.base, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPalette.base,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Vault Successfully Armed",
                                style = DisplayM.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your notifications will be guarded by ${selectedMethod.title}${if (biometricEnabled) " + Biometrics" else ""}.",
                                style = VaultBodyM.copy(fontSize = 13.sp, color = colors.textSecondary)
                            )
                        }
                    }
                }
            }

            // Bottom action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (step) {
                    SetupStep.CHOOSE_METHOD -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(ShapePill)
                                .background(colors.accent.brush())
                                .clickable {
                                    step = SetupStep.ENTER_CREDENTIAL
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Continue with ${selectedMethod.title}",
                                style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A10))
                            )
                        }
                    }
                    SetupStep.ENTER_CREDENTIAL -> {
                        if (selectedMethod == LockMethod.PASSWORD || selectedMethod == LockMethod.PIN) {
                            val canProceed = if (selectedMethod == LockMethod.PIN) {
                                enteredPin.length in 4..6
                            } else {
                                enteredPassword.length >= 4
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(ShapePill)
                                    .background(if (canProceed) colors.accent.brush() else SolidColor(colors.surface))
                                    .clickable(enabled = canProceed) {
                                        step = SetupStep.CONFIRM_CREDENTIAL
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedMethod == LockMethod.PIN) {
                                        "Confirm ${enteredPin.length}-Digit PIN"
                                    } else {
                                        "Continue"
                                    },
                                    style = VaultTitle.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canProceed) Color(0xFF0A0A10) else colors.textTertiary
                                    )
                                )
                            }
                        }
                    }
                    SetupStep.CONFIRM_CREDENTIAL -> {
                        if (selectedMethod == LockMethod.PASSWORD || selectedMethod == LockMethod.PIN) {
                            val canConfirm = confirmedPassword.isNotBlank()
                            val pinCanConfirm = confirmedPin.length == enteredPin.length
                            val confirmEnabled = if (selectedMethod == LockMethod.PIN) pinCanConfirm else canConfirm
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(ShapePill)
                                    .background(if (confirmEnabled) colors.accent.brush() else SolidColor(colors.surface))
                                    .clickable(enabled = confirmEnabled) {
                                        if (selectedMethod == LockMethod.PIN) {
                                            if (confirmedPin == enteredPin) {
                                                lockManager.setCredential(LockMethod.PIN, enteredPin)
                                                lockManager.setBiometricEnabled(biometricEnabled)
                                                step = SetupStep.SUCCESS
                                            } else {
                                                errorMessage = "PINs do not match."
                                                triggerHapticError()
                                                confirmedPin = ""
                                            }
                                        } else if (confirmedPassword == enteredPassword) {
                                            lockManager.setCredential(LockMethod.PASSWORD, enteredPassword)
                                            lockManager.setBiometricEnabled(biometricEnabled)
                                            step = SetupStep.SUCCESS
                                        } else {
                                            errorMessage = "Passwords do not match."
                                            triggerHapticError()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Confirm & Lock Vault",
                                    style = VaultTitle.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (confirmEnabled) Color(0xFF0A0A10) else colors.textTertiary
                                    )
                                )
                            }
                        }
                    }
                    SetupStep.SUCCESS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(ShapePill)
                                .background(colors.accent.brush())
                                .clickable {
                                    lockManager.unlock()
                                    onSetupComplete()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Enter NotifyVault",
                                style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A10))
                            )
                        }
                    }
                }
            }
        }
    }
}
