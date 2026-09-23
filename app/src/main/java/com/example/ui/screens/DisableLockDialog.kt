package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.security.LockManager
import com.example.security.LockMethod
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
import com.example.ui.theme.VaultTitle

@Composable
fun DisableLockDialog(
    onDismiss: () -> Unit,
    onLockDisabled: () -> Unit
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    val lockManager = remember { LockManager.getInstance(context) }
    val currentMethod = lockManager.getLockMethod()
    val currentPinLength = remember(currentMethod) { lockManager.getPinLength() }

    var credentialInput by remember { mutableStateOf("") }
    var confirmationWord by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeL,
            accentBorder = true
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CrimsonPalette.glow)
                        .border(1.5.dp, CrimsonPalette.base, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CrimsonPalette.base,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Disable Vault Lock?",
                    style = DisplayM.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your vault will no longer be protected. Anyone with your phone can read every notification.",
                    style = VaultCaption.copy(fontSize = 12.sp, color = colors.textSecondary),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Current credential input
                Text(
                    text = "ENTER CURRENT ${currentMethod.title.uppercase()}",
                    style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                when (currentMethod) {
                    LockMethod.PIN -> LockKeypad(
                        pinLength = currentPinLength,
                        currentLength = credentialInput.length,
                        onDigitClick = { digit ->
                            if (credentialInput.length < currentPinLength) {
                                credentialInput += digit
                                errorMessage = null
                            }
                        },
                        onDeleteClick = {
                            if (credentialInput.isNotEmpty()) credentialInput = credentialInput.dropLast(1)
                        }
                    )
                    LockMethod.PATTERN -> PatternLockView(
                        onPatternComplete = {
                            credentialInput = it
                            errorMessage = null
                        }
                    )
                    else -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapePill)
                            .background(colors.surface)
                            .border(1.dp, colors.cardStroke, ShapePill)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = credentialInput,
                            onValueChange = {
                                credentialInput = it
                                errorMessage = null
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                            cursorBrush = SolidColor(colors.accent.base),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (credentialInput.isEmpty()) {
                                    Text("Current ${currentMethod.title}…", style = VaultBodyM.copy(fontSize = 12.sp, color = colors.textTertiary))
                                }
                                inner()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Type "DISABLE" confirmation input
                Text(
                    text = "TYPE \"DISABLE\" TO CONFIRM",
                    style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapePill)
                        .background(colors.surface)
                        .border(1.dp, colors.cardStroke, ShapePill)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = confirmationWord,
                        onValueChange = {
                            confirmationWord = it
                            errorMessage = null
                        },
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(colors.accent.base),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (confirmationWord.isEmpty()) {
                                Text("Type DISABLE here…", style = VaultBodyM.copy(fontSize = 12.sp, color = colors.textTertiary))
                            }
                            inner()
                        }
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = VaultCaption.copy(color = CrimsonPalette.base, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val canSubmit = confirmationWord == "DISABLE" && credentialInput.isNotBlank()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(ShapePill)
                            .background(colors.surface)
                            .border(1.dp, colors.cardStroke, ShapePill)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", style = VaultTitle.copy(fontSize = 13.sp, color = colors.textSecondary))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(ShapePill)
                            .background(if (canSubmit) CrimsonPalette.base else colors.surface)
                            .clickable(enabled = canSubmit) {
                                val success = lockManager.disableLock(credentialInput)
                                if (success) {
                                    onLockDisabled()
                                } else {
                                    errorMessage = "Incorrect credential"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Disable Lock",
                            style = VaultTitle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canSubmit) Color.White else colors.textTertiary
                            )
                        )
                    }
                }
            }
        }
    }
}
