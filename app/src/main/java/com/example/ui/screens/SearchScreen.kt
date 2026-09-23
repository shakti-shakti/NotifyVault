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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppInfoResolver
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.AppIconOrb
import com.example.ui.components.AppPickerMode
import com.example.ui.components.AppPickerSheet
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
import com.example.viewmodel.DateRangeFilter
import com.example.viewmodel.VaultViewModel

@Composable
fun SearchScreen(
    viewModel: VaultViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalVaultColors.current
    val customization = LocalVaultCustomization.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.notifications.collectAsStateWithLifecycle()
    val searchDateRange by viewModel.searchDateRange.collectAsStateWithLifecycle()
    val searchTypeFilters by viewModel.searchTypeFilters.collectAsStateWithLifecycle()
    val searchSelectedApp by viewModel.searchSelectedApp.collectAsStateWithLifecycle()
    val dashboardSelectedApp by viewModel.selectedAppPackage.collectAsStateWithLifecycle()

    var showAppPickerForScope by remember { mutableStateOf(false) }

    LaunchedEffect(dashboardSelectedApp) {
        if (dashboardSelectedApp != null && searchSelectedApp == null) {
            viewModel.setSearchSelectedApp(dashboardSelectedApp)
        }
    }

    val syntaxFilters = listOf(
        "has:otp",
        "has:amount",
        "is:starred"
    )

    val recentSearches = remember {
        mutableStateListOf<String>()
    }
    val hasActiveFilters = searchQuery.isNotBlank() ||
        searchSelectedApp != null ||
        searchDateRange != DateRangeFilter.ALL ||
        searchTypeFilters.isNotEmpty()

    if (showAppPickerForScope) {
        AppPickerSheet(
            mode = AppPickerMode.SINGLE_SELECT,
            title = "Scope Search to App",
            onSingleAppSelected = { app ->
                viewModel.setSearchSelectedApp(app.packageName)
                showAppPickerForScope = false
            },
            onClose = { showAppPickerForScope = false }
        )
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
                                    text = if (searchSelectedApp != null) {
                                        val appInfo = AppInfoResolver.resolveSync(context, searchSelectedApp!!)
                                        "Search in ${appInfo.appName}…"
                                    } else {
                                        "Search OTPs, amounts, text…"
                                    },
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

            // Contextual Search Filter Bar 1: Scoped App & Date Ranges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Scope Filter Chip
                if (searchSelectedApp != null) {
                    val appInfo = remember(searchSelectedApp) {
                        AppInfoResolver.resolveSync(context, searchSelectedApp!!)
                    }
                    Box(
                        modifier = Modifier
                            .clip(ShapePill)
                            .background(colors.accent.base.copy(alpha = 0.2f))
                            .border(1.dp, colors.accent.base, ShapePill)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppIconOrb(
                                appName = appInfo.appName,
                                accentColor = colors.accent.base,
                                packageName = searchSelectedApp,
                                size = 18
                            )
                            Text(
                                text = appInfo.appName,
                                style = VaultCaption.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear App Scope",
                                tint = colors.accent.base,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.setSearchSelectedApp(null) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(ShapePill)
                            .background(colors.surface.copy(alpha = 0.6f))
                            .border(1.dp, colors.cardStroke, ShapePill)
                            .clickable { showAppPickerForScope = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Scope to App",
                                style = VaultCaption.copy(
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Date Range Chips
                DateRangeFilter.values().forEach { range ->
                    val isSelected = searchDateRange == range
                    Box(
                        modifier = Modifier
                            .clip(ShapePill)
                            .background(
                                if (isSelected) colors.accent.brush()
                                else SolidColor(colors.surface.copy(alpha = 0.6f))
                            )
                            .border(
                                1.dp,
                                if (isSelected) colors.accent.base else colors.cardStroke,
                                ShapePill
                            )
                            .clickable { viewModel.setSearchDateRange(range) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = range.label,
                            style = VaultCaption.copy(
                                color = if (isSelected) Color(0xFF0B0B12) else colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Contextual Search Filter Bar 2: Type Filters & Power Syntax
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type filters
                listOf("OTP", "Amounts", "Starred").forEach { type ->
                    val isSelected = searchTypeFilters.contains(type)
                    Box(
                        modifier = Modifier
                            .clip(ShapePill)
                            .background(
                                if (isSelected) colors.accent.base.copy(alpha = 0.25f)
                                else colors.surface.copy(alpha = 0.6f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) colors.accent.base else colors.cardStroke,
                                ShapePill
                            )
                            .clickable { viewModel.toggleSearchTypeFilter(type) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = type,
                            style = VaultCaption.copy(
                                color = if (isSelected) colors.accent.base else colors.textTertiary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Power Query Syntax Chips
                syntaxFilters.forEach { syntax ->
                    Box(
                        modifier = Modifier
                            .clip(ShapePill)
                            .background(colors.surface.copy(alpha = 0.5f))
                            .border(1.dp, colors.accent.base.copy(alpha = 0.35f), ShapePill)
                            .clickable {
                                val current = searchQuery.trim()
                                if (current.isEmpty()) {
                                    viewModel.setSearchQuery(syntax)
                                } else if (!current.contains(syntax)) {
                                    viewModel.setSearchQuery("$current $syntax")
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
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

                if (hasActiveFilters) {
                    Text(
                        text = "Clear all",
                        style = VaultCaption.copy(
                            color = colors.accent.base,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier
                            .clip(ShapePill)
                            .clickable { viewModel.clearSearchFilters() }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    )
                }
            }

            // Recent Searches (if query is empty and no custom filters)
            if (searchQuery.isEmpty() && searchTypeFilters.isEmpty() && searchSelectedApp == null) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = "SUGGESTED FILTERS",
                        style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("OTP", "Verification", "Invoice", "Payment", "Security alert").forEach { term ->
                            Box(
                                modifier = Modifier
                                    .clip(ShapePill)
                                    .background(colors.elevated.copy(alpha = 0.5f))
                                    .border(1.dp, colors.cardStroke, ShapePill)
                                    .clickable { viewModel.setSearchQuery(term) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
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
                    text = when {
                        searchQuery.isNotBlank() -> "RESULTS FOR \"$searchQuery\""
                        searchSelectedApp != null -> "FILTERED BY APP"
                        searchTypeFilters.isNotEmpty() -> "FILTERED BY ${searchTypeFilters.joinToString()}"
                        else -> "MATCHED ALERTS"
                    },
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
                    title = if (hasActiveFilters) "No Matches Found" else "No Notifications Yet",
                    description = if (hasActiveFilters) {
                        "Clear a filter or try a broader app, date, type, or text search."
                    } else {
                        "Captured alerts will appear here as NotifyVault receives them."
                    },
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

