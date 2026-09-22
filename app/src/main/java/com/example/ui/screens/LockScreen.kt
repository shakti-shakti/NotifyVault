package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricAuthHelper
import com.example.security.CooldownStatus
import com.example.security.LockManager
import com.example.security.LockMethod
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.LockKeypad
import com.example.ui.components.PatternLockView
import com.example.ui.theme.CrimsonPalette
import com.example.ui.theme.DisplayM
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    val lockManager = remember { LockManager.getInstance(context) }
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val scope = rememberCoroutineScope()

    val lockMethod by lockManager.lockMethodFlow.collectAsState()
    val isBiometricEnabled by lockManager.biometricEnabledFlow.collectAsState()

    var pinInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var resetPhraseInput by remember { mutableStateOf("") }

    var isError by remember { mutableStateOf(false) }
    var isSuccessDissolve by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    // Cooldown status
    var cooldownStatus by remember { mutableStateOf(lockManager.getCooldownStatus()) }
    var cooldownSecondsLeft by remember {
        mutableStateOf(
            (lockManager.getCooldownStatus() as? CooldownStatus.Active)?.remainingSeconds ?: 0
        )
    }

    // Trigger haptic feedback
    fun triggerHapticError() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100L)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun triggerWrongAttempt() {
        isError = true
        triggerHapticError()
        cooldownStatus = lockManager.getCooldownStatus()
        if (cooldownStatus is CooldownStatus.Active) {
            cooldownSecondsLeft = (cooldownStatus as CooldownStatus.Active).remainingSeconds
        }
        scope.launch {
            // Subtle crimson shake
            for (i in 0..2) {
                shakeOffset.animateTo(18f, animationSpec = tween(40))
                shakeOffset.animateTo(-18f, animationSpec = tween(40))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(40))
            delay(500)
            isError = false
            pinInput = ""
        }
    }

    fun handleSuccessUnlock() {
        isSuccessDissolve = true
        scope.launch {
            delay(200)
            lockManager.unlock()
            onUnlock()
        }
    }

    // Attempt Biometric Prompt on launch if configured
    LaunchedEffect(Unit) {
        if (isBiometricEnabled && BiometricAuthHelper.isBiometricAvailable(context)) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                BiometricAuthHelper.promptBiometric(
                    activity = activity,
                    onSuccess = { handleSuccessUnlock() },
                    onError = { _, _ -> },
                    onFailed = { }
                )
            }
        }
    }

    // Timer countdown for active cooldown
    LaunchedEffect(cooldownSecondsLeft) {
        if (cooldownSecondsLeft > 0) {
            delay(1000)
            cooldownSecondsLeft--
            if (cooldownSecondsLeft == 0) {
                cooldownStatus = lockManager.getCooldownStatus()
            }
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isSuccessDissolve) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isSuccessDissolve) 0f else 1f,
        animationSpec = tween(200),
        label = "alpha"
    )

    AmbientMeshBackground(
        modifier = modifier
            .scale(animatedScale)
            .alpha(animatedAlpha)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP HEADER (Lock icon + Title)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (isError) CrimsonPalette.glow else colors.accent.glow)
                        .border(1.5.dp, if (isError) CrimsonPalette.base else colors.accent.base, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = "Vault Locked",
                        tint = if (isError) CrimsonPalette.base else colors.accent.base,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "NotifyVault",
                    style = DisplayM.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        cooldownSecondsLeft > 0 -> "Too many attempts · Cooldown active"
                        cooldownStatus is CooldownStatus.RequiresResetPhrase -> "Security Lockdown"
                        isError -> "Incorrect ${lockMethod.title}. Try again."
                        else -> "Enter your ${lockMethod.title} to unlock vault"
                    },
                    style = VaultCaption.copy(
                        fontSize = 12.sp,
                        color = if (isError || cooldownSecondsLeft > 0) CrimsonPalette.base else colors.textTertiary,
                        fontWeight = if (isError || cooldownSecondsLeft > 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                )

                val failedAttempts = lockManager.getFailedAttempts()
                if (failedAttempts > 0 && cooldownSecondsLeft == 0 && cooldownStatus !is CooldownStatus.RequiresResetPhrase) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Failed attempts: $failedAttempts",
                        style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary)
                    )
                }
            }

            // 2. INPUT AREA (PIN / PASSWORD / PATTERN / COOLDOWN)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                contentAlignment = Alignment.Center
            ) {
                when {
                    cooldownSecondsLeft > 0 -> {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = ShapeL
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Temporarily Locked",
                                    style = VaultTitle.copy(color = CrimsonPalette.base, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try again in ${cooldownSecondsLeft}s",
                                    style = DisplayM.copy(fontSize = 28.sp, color = colors.textPrimary, fontFamily = FontFamily.Monospace)
                                )
                            }
                        }
                    }

                    cooldownStatus is CooldownStatus.RequiresResetPhrase -> {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = ShapeL,
                            accentBorder = true
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Maximum Attempts Exceeded",
                                    style = VaultTitle.copy(color = CrimsonPalette.base, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Type \"reset\" below to unlock cooldown",
                                    style = VaultCaption.copy(color = colors.textSecondary)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(ShapePill)
                                        .background(colors.surface)
                                        .border(1.dp, colors.cardStroke, ShapePill)
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    BasicTextField(
                                        value = resetPhraseInput,
                                        onValueChange = { resetPhraseInput = it },
                                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                                        cursorBrush = SolidColor(colors.accent.base),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            if (resetPhraseInput.isEmpty()) {
                                                Text("Type reset here…", style = VaultBodyM.copy(fontSize = 12.sp, color = colors.textTertiary))
                                            }
                                            inner()
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(ShapePill)
                                        .background(colors.accent.brush())
                                        .clickable {
                                            if (lockManager.unlockViaResetPhrase(resetPhraseInput)) {
                                                cooldownStatus = CooldownStatus.None
                                                resetPhraseInput = ""
                                            } else {
                                                triggerHapticError()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Reset Lockout", style = VaultTitle.copy(fontSize = 13.sp, color = Color(0xFF0A0A10), fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    lockMethod == LockMethod.PIN -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Top filled dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 20.dp)
                            ) {
                                for (i in 0 until 4) {
                                    val isFilled = i < pinInput.length
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isError) CrimsonPalette.base
                                                else if (isFilled) colors.accent.base
                                                else colors.surface.copy(alpha = 0.8f)
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isError) CrimsonPalette.base
                                                else if (isFilled) colors.accent.base
                                                else colors.cardStroke,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }

                            LockKeypad(
                                pinLength = 4,
                                currentLength = pinInput.length,
                                onDigitClick = { digit ->
                                    if (pinInput.length < 6) {
                                        pinInput += digit
                                        if (pinInput.length >= 4) {
                                            if (lockManager.verifyCredential(pinInput)) {
                                                handleSuccessUnlock()
                                            } else {
                                                triggerWrongAttempt()
                                            }
                                        }
                                    }
                                },
                                onDeleteClick = {
                                    if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                }
                            )
                        }
                    }

                    lockMethod == LockMethod.PASSWORD -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ShapePill)
                                    .background(colors.surface.copy(alpha = 0.85f))
                                    .border(1.dp, if (isError) CrimsonPalette.base else colors.cardStroke, ShapePill)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = passwordInput,
                                        onValueChange = {
                                            passwordInput = it
                                            isError = false
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
                                            if (passwordInput.isEmpty()) {
                                                Text("Enter password…", style = VaultBodyM.copy(color = colors.textTertiary))
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

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(ShapePill)
                                    .background(if (passwordInput.isNotEmpty()) colors.accent.brush() else SolidColor(colors.surface))
                                    .clickable(enabled = passwordInput.isNotEmpty()) {
                                        if (lockManager.verifyCredential(passwordInput)) {
                                            handleSuccessUnlock()
                                        } else {
                                            triggerWrongAttempt()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Unlock",
                                    style = VaultTitle.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (passwordInput.isNotEmpty()) Color(0xFF0A0A10) else colors.textTertiary
                                    )
                                )
                            }
                        }
                    }

                    lockMethod == LockMethod.PATTERN -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PatternLockView(
                                isEnabled = cooldownSecondsLeft == 0,
                                errorState = isError,
                                onPatternComplete = { pattern ->
                                    if (lockManager.verifyCredential(pattern)) {
                                        handleSuccessUnlock()
                                    } else {
                                        triggerWrongAttempt()
                                    }
                                }
                            )
                        }
                    }

                    else -> {
                        // Method is NONE or unset
                        LaunchedEffect(Unit) {
                            lockManager.unlock()
                            onUnlock()
                        }
                    }
                }
            }

            // 3. BOTTOM BIOMETRIC BUTTON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isBiometricEnabled && BiometricAuthHelper.isBiometricAvailable(context)) {
                    Box(
                        modifier = Modifier
                            .clip(ShapePill)
                            .background(colors.surface.copy(alpha = 0.8f))
                            .border(1.dp, colors.cardStroke, ShapePill)
                            .clickable {
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    BiometricAuthHelper.promptBiometric(
                                        activity = activity,
                                        onSuccess = { handleSuccessUnlock() },
                                        onError = { _, _ -> },
                                        onFailed = { triggerHapticError() }
                                    )
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Unlock",
                                tint = colors.accent.base,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Use Biometrics",
                                style = VaultLabel.copy(color = colors.textPrimary, fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
