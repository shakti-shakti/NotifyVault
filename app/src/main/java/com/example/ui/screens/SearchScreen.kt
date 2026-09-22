package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.EmptyState
import com.example.ui.components.GlassCard
import com.example.ui.components.NotificationCard
import com.example.ui.components.RollingNumber
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.LocalVaultCustomization
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle
import com.example.viewmodel.VaultViewModel

@Composable
fun SearchScreen(
    viewModel: VaultViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val customization = LocalVaultCustomization.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.notifications.collectAsStateWithLifecycle()

    val syntaxFilters = listOf(
        "has:otp",
        "has:amount",
        "is:starred"
    )

    val recentSearches = remember {
        mutableStateListOf<String>()
    }

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Search Input Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.8f))
                        .border(1.dp, colors.cardStroke, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = ShapePill
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.accent.base,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search OTPs, apps, text…",
                                    style = VaultBodyM.copy(color = colors.textTertiary)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(colors.accent.base),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.setSearchQuery("") }
                            )
                        }
                    }
                }
            }

            // Power Query Syntax Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                syntaxFilters.forEach { syntax ->
                    Box(
                        modifier = Modifier
                            .clip(ShapePill)
                            .background(colors.surface.copy(alpha = 0.6f))
                            .border(1.dp, colors.accent.base.copy(alpha = 0.35f), ShapePill)
                            .clickable {
                                val query = syntax.substringAfter(":")
                                viewModel.setSearchQuery(query)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = syntax,
                            style = VaultCaption.copy(
                                color = colors.accent.base,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Recent Searches (if query is empty)
            if (searchQuery.isEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "RECENT SEARCHES",
                        style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentSearches.forEach { term ->
                            Box(
                                modifier = Modifier
                                    .clip(ShapePill)
                                    .background(colors.elevated.copy(alpha = 0.5f))
                                    .border(1.dp, colors.cardStroke, ShapePill)
                                    .clickable { viewModel.setSearchQuery(term) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(term, style = VaultCaption.copy(color = colors.textSecondary))
                            }
                        }
                    }
                }
            }

            // Results count banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "RESULTS FOR \"$searchQuery\"" else "MATCHED ALERTS",
                    style = VaultLabel.copy(color = colors.textTertiary, fontSize = 11.sp)
                )
                Text(
                    text = "${searchResults.size} found",
                    style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold)
                )
            }

            // Results List
            if (searchResults.isEmpty()) {
                EmptyState(
                    title = "No Matches Found",
                    description = "Try searching by app name, OTP code, or notification title.",
                    icon = Icons.Default.Search
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = searchResults, key = { it.id }) { item ->
                        NotificationCard(
                            notification = item,
                            listStyle = customization.listStyle,
                            onClick = { onNavigateToDetail(item.id) },
                            onStarClick = { viewModel.toggleStar(item) }
                        )
                    }
                }
            }
        }
    }
}
