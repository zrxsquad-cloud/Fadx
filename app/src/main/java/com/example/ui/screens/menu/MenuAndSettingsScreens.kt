package com.example.ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseConnectionStatus
import com.example.model.*
import com.example.ui.components.AvatarImage
import com.example.ui.components.FadxBrandIcon
import com.example.ui.components.FadxLogo
import com.example.ui.theme.*

// ----------------- NOTIFICATIONS SCREEN -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onNotificationClick: (NotificationItem) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            actions = {
                TextButton(onClick = onMarkAllRead) {
                    Text("Mark all read", color = FadxPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notifications yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notif ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNotificationClick(notif) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AvatarImage(url = notif.actor.avatarUrl, name = notif.actor.name, size = 46.dp)

                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (notif.type) {
                                                NotificationType.LIKE -> FadxAccentCoral
                                                NotificationType.COMMENT -> FadxPrimary
                                                NotificationType.FRIEND_REQUEST -> FadxAccentGreen
                                                NotificationType.MENTION -> FadxSecondary
                                                else -> FadxAccentAmber
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (notif.type) {
                                            NotificationType.LIKE -> Icons.Filled.Favorite
                                            NotificationType.COMMENT -> Icons.Filled.ModeComment
                                            NotificationType.FRIEND_REQUEST -> Icons.Filled.PersonAdd
                                            NotificationType.MENTION -> Icons.Filled.AlternateEmail
                                            else -> Icons.Filled.Notifications
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.text,
                                    fontSize = 13.5.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = notif.timestamp,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!notif.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(FadxPrimary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- MENU HUB SCREEN -----------------

data class MenuShortcut(val title: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

@Composable
fun MenuHubScreen(
    currentUser: User,
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onPagesClick: () -> Unit,
    onMarketplaceClick: () -> Unit,
    onEventsClick: () -> Unit,
    onSavedClick: () -> Unit,
    onAdminClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortcuts = listOf(
        MenuShortcut("Friends", Icons.Default.People, FadxPrimary, onFriendsClick),
        MenuShortcut("Groups", Icons.Default.Groups, FadxSecondary, onGroupsClick),
        MenuShortcut("Pages", Icons.Default.Flag, FadxAccentCoral, onPagesClick),
        MenuShortcut("Marketplace", Icons.Default.Storefront, FadxAccentGreen, onMarketplaceClick),
        MenuShortcut("Events", Icons.Default.Event, FadxAccentAmber, onEventsClick),
        MenuShortcut("Saved", Icons.Default.Bookmark, FadxPrimary, onSavedClick),
        MenuShortcut("Admin Panel", Icons.Default.AdminPanelSettings, FadxSecondary, onAdminClick),
        MenuShortcut("Settings", Icons.Default.Settings, Color.Gray, onSettingsClick)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Menu", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        // Current User Profile Header Box
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AvatarImage(url = currentUser.avatarUrl, name = currentUser.name, size = 52.dp)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentUser.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("View and edit your profile", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 2-Column Grid of Shortcuts
        item {
            Text("All Shortcuts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shortcuts) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { item.onClick() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(item.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = item.title, tint = item.color, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Brand Info Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FadxBrandIcon(size = 42.dp, cornerRadius = 11.dp)

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fadx Social App", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Version 2.4.0 • Build 2026.1", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("The modern platform to connect and inspire", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Logout Button
        item {
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Log out", tint = FadxAccentCoral)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out of Fadx", color = FadxAccentCoral, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------- SETTINGS & PRIVACY SCREEN -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    twoFactorEnabled: Boolean,
    onToggle2FA: () -> Unit,
    supabaseStatus: SupabaseConnectionStatus = SupabaseConnectionStatus.IDLE,
    onTestSupabase: () -> Unit = {},
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Appearance & Theme", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Theme Preference", fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = currentTheme == "SYSTEM",
                                    onClick = { onThemeChange("SYSTEM") },
                                    label = { Text("System") }
                                )
                                FilterChip(
                                    selected = currentTheme == "DARK",
                                    onClick = { onThemeChange("DARK") },
                                    label = { Text("Dark") }
                                )
                                FilterChip(
                                    selected = currentTheme == "LIGHT",
                                    onClick = { onThemeChange("LIGHT") },
                                    label = { Text("Light") }
                                )
                            }
                        }
                    }
                }
            }

            // Supabase Cloud Backend Configuration
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = "Supabase",
                                    tint = FadxSecondary
                                )
                                Text("Supabase Cloud Backend", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            // Status Indicator Chip
                            val (badgeText, badgeBg, badgeTextColor) = when (supabaseStatus) {
                                SupabaseConnectionStatus.CONNECTED -> Triple("Active 🟢", Color(0xFF1B5E20).copy(alpha = 0.2f), Color(0xFF4CAF50))
                                SupabaseConnectionStatus.CONNECTING -> Triple("Checking...", Color(0xFFF57F17).copy(alpha = 0.2f), Color(0xFFFFB300))
                                SupabaseConnectionStatus.ERROR -> Triple("Issue ⚠️", Color(0xFFB71C1C).copy(alpha = 0.2f), Color(0xFFEF5350))
                                SupabaseConnectionStatus.IDLE -> Triple("Configured", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "URL: ${SupabaseConfig.url}",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Key: ${SupabaseConfig.publishableKey.take(16)}...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = onTestSupabase,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Test connection", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Supabase Connection", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Security & 2FA
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Security & Login", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Two-Factor Authentication (2FA)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Add extra security requiring OTP on new device logins", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = twoFactorEnabled,
                                onCheckedChange = { onToggle2FA() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FadxPrimary)
                            )
                        }
                    }
                }
            }

            // Privacy Defaults
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Privacy Controls", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("• Default Post Audience: Public", fontSize = 13.sp)
                        Text("• Who can send friend requests: Everyone", fontSize = 13.sp)
                        Text("• Profile search visibility: Enabled", fontSize = 13.sp)
                        Text("• Active Online Status: Visible to friends", fontSize = 13.sp)
                    }
                }
            }

            // About Fadx & Branding
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FadxBrandIcon(size = 50.dp, cornerRadius = 14.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Fadx", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Official Version 2.4.0 (2026)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Privacy Policy • Terms of Service • Community Standards",
                            fontSize = 11.5.sp,
                            color = FadxPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ----------------- ADMIN & MODERATION PANEL -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    reports: List<ReportItem>,
    onResolveReport: (String, String) -> Unit,
    onDismissReport: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin & Moderation Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Platform Stats Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Platform Health & Metrics", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("98.4%", fontWeight = FontWeight.Black, fontSize = 18.sp, color = FadxAccentGreen)
                                Text("Uptime", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${reports.size}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = FadxAccentCoral)
                                Text("Pending Reports", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("2.4k", fontWeight = FontWeight.Black, fontSize = 18.sp, color = FadxPrimary)
                                Text("Active DAU", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Text("Reported Content Queue (${reports.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (reports.isEmpty()) {
                item {
                    Text("No pending reports in the queue 🎉", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(reports) { report ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Report Type: ${report.targetType.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = FadxAccentCoral
                                )
                                Text(
                                    text = report.timestamp,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Reason: ${report.reason.label}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Content snippet: \"${report.targetSummary}\"", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onResolveReport(report.id, "Content Removed") },
                                    colors = ButtonDefaults.buttonColors(containerColor = FadxAccentCoral),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Remove Content", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onDismissReport(report.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Dismiss", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
