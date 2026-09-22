package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.AnimatedAreaChart
import com.example.ui.components.AppIconOrb
import com.example.ui.components.DonutChart
import com.example.ui.components.GlassCard
import com.example.ui.components.RadialHeatmap
import com.example.ui.components.RollingNumber
import com.example.ui.theme.DisplayM
import com.example.ui.theme.EmeraldPalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle
import com.example.viewmodel.VaultViewModel

@Composable
fun InsightsScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val otpCount by viewModel.otpCount.collectAsStateWithLifecycle()
    val paymentCount by viewModel.paymentCount.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()

    // 100% Real Analytics calculated from local Room database
    val activityData by viewModel.realDailyActivity.collectAsStateWithLifecycle()
    val hourlyCounts by viewModel.realHourlyCounts.collectAsStateWithLifecycle()
    val categorySlices by viewModel.realCategorySlices.collectAsStateWithLifecycle()
    val topApps by viewModel.realTopApps.collectAsStateWithLifecycle()

    val peakHour = remember(hourlyCounts) {
        val max = hourlyCounts.maxOrNull() ?: 0
        if (max > 0) hourlyCounts.indexOf(max) else 12
    }

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            // Screen Header
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = "Insights",
                    style = DisplayM.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "Real Device Behavioral Analytics",
                    style = VaultCaption.copy(fontSize = 12.sp, color = colors.textTertiary)
                )
            }

            // HERO STAT CARD
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = ShapeL,
                accentBorder = true
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TOTAL ARCHIVED NOTIFICATIONS",
                        style = VaultLabel.copy(fontSize = 10.sp, color = colors.accent.base, letterSpacing = 0.1.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    RollingNumber(targetNumber = totalCount)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(3.dp)
                            .clip(ShapePill)
                            .background(colors.accent.brush())
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$todayCount alerts captured today. Processed 100% on-device with zero-knowledge local storage.",
                        style = VaultBodyM.copy(fontSize = 12.sp, color = colors.textSecondary)
                    )
                }
            }

            // 14-DAY INFLOW TRAJECTORY
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "14-DAY INFLOW TRAJECTORY",
                        style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                    )
                    Text(
                        text = "Real Counts",
                        style = VaultCaption.copy(fontSize = 11.sp, color = colors.accent.base)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeL
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (totalCount == 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No notification traffic recorded yet.\nReal alerts from your device will plot here automatically.",
                                    style = VaultCaption.copy(color = colors.textTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                )
                            }
                        } else {
                            AnimatedAreaChart(dataPoints = activityData)
                        }
                    }
                }
            }

            // RADIAL HEATMAP & CATEGORY BREAKDOWN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Radial Heatmap
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = ShapeL
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BUSIEST HOURS",
                            style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        RadialHeatmap(hourlyCounts = hourlyCounts, peakHour = peakHour)
                    }
                }

                // Donut Chart
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = ShapeL
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CATEGORIES",
                            style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (categorySlices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Awaiting Alerts",
                                    style = VaultCaption.copy(color = colors.textTertiary, fontSize = 11.sp)
                                )
                            }
                        } else {
                            DonutChart(slices = categorySlices)
                        }
                    }
                }
            }

            // REAL TOP APPLICATIONS
            if (topApps.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(
                        text = "TOP ALERT SOURCES",
                        style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapeL
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            topApps.forEachIndexed { index, stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        AppIconOrb(
                                            appName = stat.appName,
                                            accentColor = colors.accent.base,
                                            iconPath = stat.iconPath,
                                            packageName = stat.packageName,
                                            size = 28
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stat.appName,
                                            style = VaultBodyM.copy(color = colors.textPrimary, fontWeight = FontWeight.Medium)
                                        )
                                    }
                                    Text(
                                        text = "${stat.count} alerts",
                                        style = VaultCaption.copy(color = colors.textSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // NOTIFICATION PROFILE (DYNAMIC FROM REAL DATA)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = ShapeL
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(EmeraldPalette.glow)
                            .border(1.dp, EmeraldPalette.base, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmeraldPalette.base,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    val (personalityTitle, personalityDesc) = remember(totalCount, otpCount, paymentCount) {
                        when {
                            totalCount == 0 -> "The Silent Sovereign" to "Your archive is currently quiet. Any alert that hits your status bar will be cataloged instantly."
                            otpCount + paymentCount > totalCount / 3 -> "The Financial Sentinel" to "High concentration of banking transactions and OTP verifications cataloged with on-device encryption."
                            else -> "The Active Communicator" to "$totalCount total alerts received and securely organized into your local vault."
                        }
                    }

                    Column {
                        Text(
                            text = "NOTIFICATION PROFILE",
                            style = VaultLabel.copy(fontSize = 9.sp, color = EmeraldPalette.base)
                        )
                        Text(
                            text = personalityTitle,
                            style = VaultTitle.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = personalityDesc,
                            style = VaultBodyM.copy(fontSize = 12.sp, color = colors.textSecondary)
                        )
                    }
                }
            }

            // REAL ON-DEVICE SUMMARY CARD
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = ShapeL
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "REAL-TIME ARCHIVE STATUS",
                        style = VaultLabel.copy(fontSize = 10.sp, color = colors.accent.base)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$totalCount Real Alerts Stored",
                        style = VaultTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "NotifyVault has captured $otpCount OTP passcodes and $paymentCount payment confirmations directly from your Android status bar. All records reside exclusively on this device.",
                        style = VaultBodyM.copy(fontSize = 13.sp, color = colors.textSecondary)
                    )
                }
            }
        }
    }
}
