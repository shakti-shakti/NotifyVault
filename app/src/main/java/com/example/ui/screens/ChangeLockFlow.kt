package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.delay

enum class ChangeStep {
    VERIFY_OLD,
    CHOOSE_NEW_METHOD,
    ENTER_NEW,
    CONFIRM_NEW,
    SUCCESS
}

@Composable
fun ChangeLockFlow(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    val lockManager = remember { LockManager.getInstance(context) }
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    val currentMethod = lockManager.getLockMethod()
    var step by remember { mutableStateOf(ChangeStep.VERIFY_OLD) }
    var selectedNewMethod by remember { mutableStateOf(LockMethod.PIN) }

    // Old verify inputs
    var oldPinInput by remember { mutableStateOf("") }
    var oldPasswordInput by remember { mutableStateOf("") }
    var verifyAttempts by remember { mutableStateOf(0) }
    var cooldownRemaining by remember { mutableStateOf(0) }

    // New inputs
    var newPinInput by remember { mutableStateOf("") }
    var confirmedNewPinInput by remember { mutableStateOf("") }

    var newPasswordInput by remember { mutableStateOf("") }
    var confirmedNewPasswordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var newPatternInput by remember { mutableStateOf("") }
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

    LaunchedEffect(cooldownRemaining) {
        if (cooldownRemaining > 0) {
            delay(1000)
            cooldownRemaining--
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
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.8f))
                        .border(1.dp, colors.cardStroke, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }

                Text(
                    text = when (step) {
                        ChangeStep.VERIFY_OLD -> "Verify Identity"
                        ChangeStep.CHOOSE_NEW_METHOD -> "Choose New Method"
                        ChangeStep.ENTER_NEW -> "New ${selectedNewMethod.title}"
                        ChangeStep.CONFIRM_NEW -> "Confirm New ${selectedNewMethod.title}"
                        ChangeStep.SUCCESS -> "Lock Updated"
                    },
                    style = DisplayM.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = VaultCaption.copy(color = CrimsonPalette.base, fontWeight = FontWeight.SemiBold)
                )
            }

            // Step Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    ChangeStep.VERIFY_OLD -> {
                        if (cooldownRemaining > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Too many failed attempts",
                                    style = VaultTitle.copy(color = CrimsonPalette.base, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try again in ${cooldownRemaining}s",
                                    style = VaultCaption.copy(color = colors.textTertiary)
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Enter current ${currentMethod.title}",
                                    style = VaultBodyM.copy(color = colors.textSecondary)
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                when (currentMethod) {
                                    LockMethod.PIN -> {
                                        LockKeypad(
                                            pinLength = 4,
                                            currentLength = oldPinInput.length,
                                            onDigitClick = { d ->
                                                if (oldPinInput.length < 6) {
                                                    oldPinInput += d
                                                    errorMessage = null
                                                    if (oldPinInput.length >= 4) {
                                                        if (lockManager.verifyCredential(oldPinInput)) {
                                                            step = ChangeStep.CHOOSE_NEW_METHOD
                                                        } else {
                                                            verifyAttempts++
                                                            triggerHapticError()
                                                            oldPinInput = ""
                                                            if (verifyAttempts >= 3) {
                                                                cooldownRemaining = 60
                                                                verifyAttempts = 0
                                                            } else {
                                                                errorMessage = "Incorrect PIN (${3 - verifyAttempts} attempts left)"
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onDeleteClick = {
                                                if (oldPinInput.isNotEmpty()) oldPinInput = oldPinInput.dropLast(1)
                                            }
                                        )
                                    }
                                    LockMethod.PASSWORD -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(ShapePill)
                                                .background(colors.surface)
                                                .border(1.dp, colors.cardStroke, ShapePill)
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            BasicTextField(
                                                value = oldPasswordInput,
                                                onValueChange = {
                                                    oldPasswordInput = it
                                                    errorMessage = null
                                                },
                                                visualTransformation = PasswordVisualTransformation(),
                                                textStyle = TextStyle(color = colors.textPrimary, fontSize = 16.sp, fontFamily = FontFamily.Monospace),
                                                cursorBrush = SolidColor(colors.accent.base),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                decorationBox = { inner ->
                                                    if (oldPasswordInput.isEmpty()) {
                                                        Text("Enter current password…", style = VaultBodyM.copy(color = colors.textTertiary))
                                                    }
                                                    inner()
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .clip(ShapePill)
                                                .background(colors.accent.brush())
                                                .clickable {
                                                    if (lockManager.verifyCredential(oldPasswordInput)) {
                                                        step = ChangeStep.CHOOSE_NEW_METHOD
                                                    } else {
                                                        verifyAttempts++
                                                        triggerHapticError()
                                                        if (verifyAttempts >= 3) {
                                                            cooldownRemaining = 60
                                                            verifyAttempts = 0
                                                        } else {
                                                            errorMessage = "Incorrect password (${3 - verifyAttempts} left)"
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Verify", style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A10)))
                                        }
                                    }
                                    LockMethod.PATTERN -> {
                                        PatternLockView(
                                            onPatternComplete = { pattern ->
                                                if (lockManager.verifyCredential(pattern)) {
                                                    step = ChangeStep.CHOOSE_NEW_METHOD
                                                } else {
                                                    verifyAttempts++
                                                    triggerHapticError()
                                                    if (verifyAttempts >= 3) {
                                                        cooldownRemaining = 60
                                                        verifyAttempts = 0
                                                    } else {
                                                        errorMessage = "Incorrect pattern (${3 - verifyAttempts} left)"
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    else -> {
                                        step = ChangeStep.CHOOSE_NEW_METHOD
                                    }
                                }
                            }
                        }
                    }

                    ChangeStep.CHOOSE_NEW_METHOD -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            listOf(
                                LockMethod.PIN to "4–6 Digit PIN",
                                LockMethod.PASSWORD to "Alphanumeric Password",
                                LockMethod.PATTERN to "3x3 Pattern Lock"
                            ).forEach { (m, label) ->
                                val isSelected = selectedNewMethod == m
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    accentBorder = isSelected,
                                    onClick = { selectedNewMethod = m }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = label, style = VaultTitle.copy(fontSize = 15.sp, color = colors.textPrimary))
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, null, tint = colors.accent.base, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(ShapePill)
                                    .background(colors.accent.brush())
                                    .clickable { step = ChangeStep.ENTER_NEW },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Continue", style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A10)))
                            }
                        }
                    }

                    ChangeStep.ENTER_NEW -> {
                        when (selectedNewMethod) {
                            LockMethod.PIN -> {
                                LockKeypad(
                                    pinLength = 4,
                                    currentLength = newPinInput.length,
                                    onDigitClick = { d ->
                                        if (newPinInput.length < 6) {
                                            newPinInput += d
                                            if (newPinInput.length >= 4) {
                                                step = ChangeStep.CONFIRM_NEW
                                            }
                                        }
                                    },
                                    onDeleteClick = { if (newPinInput.isNotEmpty()) newPinInput = newPinInput.dropLast(1) }
                                )
                            }
                            LockMethod.PASSWORD -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(ShapePill)
                                            .background(colors.surface)
                                            .border(1.dp, colors.cardStroke, ShapePill)
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        BasicTextField(
                                            value = newPasswordInput,
                                            onValueChange = { newPasswordInput = it },
                                            textStyle = TextStyle(color = colors.textPrimary, fontSize = 16.sp, fontFamily = FontFamily.Monospace),
                                            cursorBrush = SolidColor(colors.accent.base),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            decorationBox = { inner ->
                                                if (newPasswordInput.isEmpty()) Text("Enter new password…", style = VaultBodyM.copy(color = colors.textTertiary))
                                                inner()
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(ShapePill)
                                            .background(if (newPasswordInput.length >= 4) colors.accent.brush() else SolidColor(colors.surface))
                                            .clickable(enabled = newPasswordInput.length >= 4) { step = ChangeStep.CONFIRM_NEW },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Continue", style = VaultTitle.copy(fontSize = 14.sp, color = Color(0xFF0A0A10), fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                            LockMethod.PATTERN -> {
                                PatternLockView(
                                    onPatternComplete = { p ->
                                        if (p.split("-").size >= 4) {
                                            newPatternInput = p
                                            step = ChangeStep.CONFIRM_NEW
                                        } else {
                                            errorMessage = "Connect at least 4 dots"
                                            triggerHapticError()
                                        }
                                    }
                                )
                            }
                            else -> {}
                        }
                    }

                    ChangeStep.CONFIRM_NEW -> {
                        when (selectedNewMethod) {
                            LockMethod.PIN -> {
                                LockKeypad(
                                    pinLength = 4,
                                    currentLength = confirmedNewPinInput.length,
                                    onDigitClick = { d ->
                                        if (confirmedNewPinInput.length < 6) {
                                            confirmedNewPinInput += d
                                            if (confirmedNewPinInput.length == newPinInput.length) {
                                                if (confirmedNewPinInput == newPinInput) {
                                                    lockManager.setCredential(LockMethod.PIN, newPinInput)
                                                    step = ChangeStep.SUCCESS
                                                } else {
                                                    errorMessage = "PINs do not match"
                                                    triggerHapticError()
                                                    confirmedNewPinInput = ""
                                                }
                                            }
                                        }
                                    },
                                    onDeleteClick = { if (confirmedNewPinInput.isNotEmpty()) confirmedNewPinInput = confirmedNewPinInput.dropLast(1) }
                                )
                            }
                            LockMethod.PASSWORD -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(ShapePill)
                                            .background(colors.surface)
                                            .border(1.dp, colors.cardStroke, ShapePill)
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        BasicTextField(
                                            value = confirmedNewPasswordInput,
                                            onValueChange = { confirmedNewPasswordInput = it },
                                            textStyle = TextStyle(color = colors.textPrimary, fontSize = 16.sp, fontFamily = FontFamily.Monospace),
                                            cursorBrush = SolidColor(colors.accent.base),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            decorationBox = { inner ->
                                                if (confirmedNewPasswordInput.isEmpty()) Text("Re-enter new password…", style = VaultBodyM.copy(color = colors.textTertiary))
                                                inner()
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(ShapePill)
                                            .background(colors.accent.brush())
                                            .clickable {
                                                if (confirmedNewPasswordInput == newPasswordInput) {
                                                    lockManager.setCredential(LockMethod.PASSWORD, newPasswordInput)
                                                    step = ChangeStep.SUCCESS
                                                } else {
                                                    errorMessage = "Passwords do not match"
                                                    triggerHapticError()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Save New Password", style = VaultTitle.copy(fontSize = 14.sp, color = Color(0xFF0A0A10), fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                            LockMethod.PATTERN -> {
                                PatternLockView(
                                    onPatternComplete = { p ->
                                        if (p == newPatternInput) {
                                            lockManager.setCredential(LockMethod.PATTERN, p)
                                            step = ChangeStep.SUCCESS
                                        } else {
                                            errorMessage = "Patterns do not match"
                                            triggerHapticError()
                                        }
                                    }
                                )
                            }
                            else -> {}
                        }
                    }

                    ChangeStep.SUCCESS -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPalette.glow)
                                    .border(2.dp, EmeraldPalette.base, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = EmeraldPalette.base, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Lock Credential Updated", style = DisplayM.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(24.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(ShapePill)
                                    .background(colors.accent.brush())
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Done", style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A10)))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
