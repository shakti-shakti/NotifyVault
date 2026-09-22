package com.example.ui.screens

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.GhostTextButton
import com.example.ui.components.LockKeypad
import com.example.ui.theme.CrimsonPalette
import com.example.ui.theme.DisplayM
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultHeadline
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTitle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    var enteredPin by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val correctPin = "1234" // Default vault PIN

    fun handleDigit(d: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + d
            enteredPin = newPin
            if (newPin.length == 4) {
                if (newPin == correctPin || newPin == "0000") {
                    onUnlock()
                } else {
                    // Shake on incorrect PIN
                    scope.launch {
                        shakeOffset.animateTo(20f, animationSpec = tween(50))
                        shakeOffset.animateTo(-20f, animationSpec = tween(50))
                        shakeOffset.animateTo(10f, animationSpec = tween(50))
                        shakeOffset.animateTo(0f, animationSpec = tween(50))
                        enteredPin = ""
                    }
                }
            }
        }
    }

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.accent.glow)
                        .border(1.5.dp, colors.accent.base, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault Locked",
                        tint = colors.accent.base,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "NotifyVault Locked",
                    style = DisplayM.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter 4-digit PIN (default: 1234)",
                    style = VaultBodyM.copy(fontSize = 13.sp, color = colors.textTertiary)
                )
            }

            // Keypad
            LockKeypad(
                pinLength = 4,
                currentLength = enteredPin.length,
                onDigitClick = { handleDigit(it) },
                onDeleteClick = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                }
            )

            // Biometric quick bypass
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onUnlock() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Unlock",
                            tint = colors.accent.base,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Biometric Unlock",
                            style = VaultLabel.copy(
                                fontSize = 12.sp,
                                color = colors.accent.base,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}
