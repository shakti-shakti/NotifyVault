package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationEntity
import com.example.ui.theme.AurumPalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.LocalVaultCustomization
import com.example.ui.theme.NotifyVaultTheme
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapeM
import com.example.ui.theme.ShapePill
import com.example.ui.theme.ShapeS
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultHeadline
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTitle
import com.example.ui.theme.getCategoryPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCard(
    notification: NotificationEntity,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    listStyle: String = "Cards",
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onStarClick: (() -> Unit)? = null,
    onCopyOtpClick: ((String) -> Unit)? = null
) {
    val colors = LocalVaultColors.current
    val categoryPalette = getCategoryPalette(notification.category)
    val appColor = categoryPalette.base

    when (listStyle) {
        "Timeline" -> TimelineNotificationRow(notification, categoryPalette, onClick)
        "Bubbles" -> BubbleNotificationCard(notification, categoryPalette, onClick)
        "Compact" -> CompactNotificationRow(notification, categoryPalette, isSelected, isSelectionMode, onClick)
        "Magazine" -> MagazineGridCard(notification, categoryPalette, onClick)
        else -> DefaultNotificationCard(
            notification = notification,
            appColor = appColor,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            onClick = onClick,
            onStarClick = onStarClick,
            onCopyOtpClick = onCopyOtpClick
        )
    }
}

// 2.3 DEFAULT CARD
@Composable
private fun DefaultNotificationCard(
    notification: NotificationEntity,
    appColor: Color,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onStarClick: (() -> Unit)?,
    onCopyOtpClick: ((String) -> Unit)?
) {
    val colors = LocalVaultColors.current
    val categoryPalette = getCategoryPalette(notification.category)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .instantTap(onLongClick = onLongClick, onClick = onClick),
        shape = ShapeL,
        accentBorder = isSelected || notification.isStarred
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left 3dp vertical accent bar
            AccentBar(
                color = appColor,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Row: App Icon + App Name + Spacer + Time + Unread indicator / Star
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppIconOrb(
                        appName = notification.appName,
                        accentColor = appColor,
                        iconPath = notification.appIconPath,
                        packageName = notification.packageName,
                        size = 32
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = notification.appName.uppercase(),
                        style = VaultLabel.copy(
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = formatTimeAgo(notification.captureTime),
                        style = VaultCaption.copy(
                            color = colors.textTertiary,
                            fontSize = 11.sp
                        )
                    )

                    if (isSelectionMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.accent.base else Color.Transparent)
                                .border(1.5.dp, colors.accent.base, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    } else {
                        // Star action button
                        if (onStarClick != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (notification.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Star",
                                tint = if (notification.isStarred) AurumPalette.base else colors.textTertiary.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onStarClick
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title (Title, primary, max 1 line, ellipsis)
                if (!notification.title.isNullOrBlank()) {
                    Text(
                        text = notification.title,
                        style = VaultTitle.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Body (Body M, secondary, max 2 lines, ellipsis)
                val bodyText = notification.bigText ?: notification.text
                if (!bodyText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bodyText,
                        style = VaultBodyM.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badges Row
                val hasBadges = notification.hasOtp || notification.hasAmount || notification.hasLink || notification.actionLabels.isNotBlank()
                if (hasBadges) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (notification.hasOtp && notification.otpCode != null) {
                            BadgePill(
                                label = "OTP: ${notification.otpCode}",
                                color = categoryPalette.base,
                                onClick = { onCopyOtpClick?.invoke(notification.otpCode) }
                            )
                        }
                        if (notification.hasAmount && notification.amountString != null) {
                            BadgePill(
                                label = notification.amountString,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                        if (notification.hasLink) {
                            BadgePill(
                                label = "LINK",
                                color = Color(0xFF818CF8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(
    label: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    val clickMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = Modifier
            .clip(ShapePill)
            .background(color.copy(alpha = 0.15f))
            .border(0.75.dp, color.copy(alpha = 0.5f), ShapePill)
            .then(clickMod)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = VaultCaption.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

// 2.3 Alternate: TIMELINE
@Composable
private fun TimelineNotificationRow(
    notification: NotificationEntity,
    categoryPalette: com.example.ui.theme.AccentPalette,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .instantTap(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        // Time Rail
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(52.dp)
        ) {
            Text(
                text = formatTimeOnly(notification.captureTime),
                style = VaultCaption.copy(
                    fontSize = 11.sp,
                    color = colors.textTertiary,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Timeline dot & line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(categoryPalette.base)
                    .border(1.5.dp, colors.surface, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(60.dp)
                    .background(colors.cardStroke)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content Glass Card
        GlassCard(
            modifier = Modifier.weight(1f),
            shape = ShapeM
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = notification.appName.uppercase(),
                    style = VaultLabel.copy(fontSize = 10.sp, color = categoryPalette.base)
                )
                Text(
                    text = notification.title ?: "Notification",
                    style = VaultTitle.copy(fontSize = 14.sp),
                    color = colors.textPrimary,
                    maxLines = 1
                )
                Text(
                    text = notification.text ?: "",
                    style = VaultBodyM.copy(fontSize = 12.sp),
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// 2.3 Alternate: CHAT BUBBLE
@Composable
private fun BubbleNotificationCard(
    notification: NotificationEntity,
    categoryPalette: com.example.ui.theme.AccentPalette,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .instantTap(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        AppIconOrb(
            appName = notification.appName,
            accentColor = categoryPalette.base,
            iconPath = notification.appIconPath,
            packageName = notification.packageName,
            size = 34
        )
        Spacer(modifier = Modifier.width(10.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = ShapeM
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notification.senderName ?: notification.appName,
                        style = VaultLabel.copy(color = categoryPalette.base, fontSize = 11.sp)
                    )
                    Text(
                        text = formatTimeAgo(notification.captureTime),
                        style = VaultCaption.copy(color = colors.textTertiary, fontSize = 10.sp)
                    )
                }
                Text(
                    text = notification.title ?: "",
                    style = VaultTitle.copy(fontSize = 14.sp),
                    color = colors.textPrimary
                )
                Text(
                    text = notification.text ?: "",
                    style = VaultBodyM.copy(fontSize = 12.sp),
                    color = colors.textSecondary
                )
            }
        }
    }
}

// 2.3 Alternate: COMPACT
@Composable
private fun CompactNotificationRow(
    notification: NotificationEntity,
    categoryPalette: com.example.ui.theme.AccentPalette,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .instantTap(onClick = onClick),
        shape = ShapeM,
        accentBorder = isSelected
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconOrb(
                appName = notification.appName,
                accentColor = categoryPalette.base,
                iconPath = notification.appIconPath,
                packageName = notification.packageName,
                size = 28
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title ?: notification.appName,
                    style = VaultTitle.copy(fontSize = 13.sp),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = notification.text ?: "",
                    style = VaultCaption.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatTimeAgo(notification.captureTime),
                style = VaultCaption.copy(color = colors.textTertiary, fontSize = 10.sp)
            )
        }
    }
}

// 2.3 Alternate: MAGAZINE GRID CARD
@Composable
private fun MagazineGridCard(
    notification: NotificationEntity,
    categoryPalette: com.example.ui.theme.AccentPalette,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .instantTap(onClick = onClick),
        shape = ShapeL
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AppIconOrb(
                appName = notification.appName,
                accentColor = categoryPalette.base,
                iconPath = notification.appIconPath,
                packageName = notification.packageName,
                size = 44
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = notification.appName.uppercase(),
                style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary)
            )
            Text(
                text = notification.title ?: "",
                style = VaultHeadline.copy(fontSize = 17.sp),
                color = colors.textPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = notification.text ?: "",
                style = VaultBodyM.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                maxLines = 3
            )
        }
    }
}

// Group Stack Component (+X more cards)
@Composable
fun NotificationStack(
    count: Int,
    appName: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Fanned back cards
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(48.dp)
                .clip(ShapeL)
                .background(colors.elevated.copy(alpha = 0.4f))
                .border(1.dp, colors.cardStroke, ShapeL)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(42.dp)
                .clip(ShapeL)
                .background(colors.elevated.copy(alpha = 0.6f))
                .border(1.dp, colors.cardStroke, ShapeL)
        )
        // Main stack header
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = ShapePill
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$appName Stack",
                    style = VaultLabel.copy(color = colors.textPrimary, fontSize = 12.sp)
                )
                Text(
                    text = "+$count more",
                    style = VaultCaption.copy(
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

fun formatTimeAgo(timeMs: Long): String {
    val diff = System.currentTimeMillis() - timeMs
    val sec = diff / 1000
    val min = sec / 60
    val hr = min / 60
    val day = hr / 24

    return when {
        min < 1 -> "Just now"
        min < 60 -> "${min}m ago"
        hr < 24 -> "${hr}h ago"
        day < 7 -> "${day}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timeMs))
    }
}

fun formatTimeOnly(timeMs: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
}

@Preview
@Composable
fun NotificationCardPreview() {
    NotifyVaultTheme {
        NotificationCard(
            notification = NotificationEntity(
                id = 1,
                packageName = "com.example",
                appName = "NotifyVault",
                title = "Security Alert",
                text = "Live notification monitoring active."
            ),
            onClick = {}
        )
    }
}
