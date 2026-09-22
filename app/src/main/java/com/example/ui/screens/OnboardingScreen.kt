package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.GhostTextButton
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimaryPillButton
import com.example.ui.theme.DisplayM
import com.example.ui.theme.EmeraldPalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyL
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPageData(
            title = "Every notification, kept.",
            subtitle = "Automatic continuous capture of all incoming alerts, OTPs, receipts, and chats into an encrypted vault.",
            icon = Icons.Default.Archive
        ),
        OnboardingPageData(
            title = "Searchable. Forever.",
            subtitle = "Zero-latency query engine with regex OTP extraction, payment tagging, and instant deep filtering.",
            icon = Icons.Default.Search
        ),
        OnboardingPageData(
            title = "Private by design.",
            subtitle = "100% on-device SQLite Room database. No third-party analytics. No cloud upload. Complete biometric confidentiality.",
            icon = Icons.Default.Shield
        )
    )

    val currentData = pages[currentPage]

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTIFYVAULT",
                    style = VaultLabel.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        color = colors.accent.base
                    )
                )
                GhostTextButton(
                    text = "Skip",
                    onClick = onFinish,
                    color = colors.textTertiary
                )
            }

            // Middle Hero Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(colors.accent.glow)
                        .border(1.5.dp, colors.accent.base, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentData.icon,
                        contentDescription = null,
                        tint = colors.accent.base,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = currentData.title,
                    style = DisplayM.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentData.subtitle,
                    style = VaultBodyL.copy(
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = colors.textSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                if (currentPage == 2) {
                    Spacer(modifier = Modifier.height(20.dp))
                    // Direct shortcut to grant Notification Access
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            },
                        shape = ShapePill
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, tint = colors.accent.base, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Grant Notification Access in Settings",
                                style = VaultCaption.copy(fontWeight = FontWeight.Bold, color = colors.accent.base)
                            )
                        }
                    }
                }
            }

            // Bottom Navigation & CTA
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Capsules
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = index == currentPage
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 28.dp else 8.dp)
                                .height(8.dp)
                                .clip(ShapePill)
                                .background(if (isSelected) colors.accent.base else colors.cardStroke)
                        )
                    }
                }

                PrimaryPillButton(
                    text = if (currentPage < pages.size - 1) "Continue" else "Enter NotifyVault",
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onFinish()
                        }
                    }
                )
            }
        }
    }
}

private data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)
