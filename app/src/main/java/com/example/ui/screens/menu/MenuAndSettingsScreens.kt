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
    onTestPush: () -> Unit = {},
    onTestLike: () -> Unit = {},
    onTestComment: () -> Unit = {},
    onTestShare: () -> Unit = {},
    onTestMessage: () -> Unit = {},
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
                IconButton(onClick = onTestPush) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Test Push Notification",
                        tint = FadxPrimary
                    )
                }
                TextButton(onClick = onMarkAllRead) {
                    Text("Mark all read", color = FadxPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )

        // Notification Quick Test & Simulation Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Simulate Live Notifications:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = onTestLike,
                    label = { Text("❤️ Like", fontSize = 11.5.sp) },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onTestComment,
                    label = { Text("💬 Comment", fontSize = 11.5.sp) },
                    modifier = Modifier.weight(1.1f)
                )
                AssistChip(
                    onClick = onTestShare,
                    label = { Text("↗️ Share", fontSize = 11.5.sp) },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onTestMessage,
                    label = { Text("✉️ Chat", fontSize = 11.5.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (notif.type) {
                                                NotificationType.LIKE -> FadxAccentCoral
                                                NotificationType.COMMENT, NotificationType.REPLY -> FadxPrimary
                                                NotificationType.SHARE -> Color(0xFF8B5CF6)
                                                NotificationType.MESSAGE -> FadxSecondary
                                                NotificationType.FRIEND_REQUEST, NotificationType.FRIEND_ACCEPT -> FadxAccentGreen
                                                NotificationType.MENTION -> FadxSecondary
                                                else -> FadxAccentAmber
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (notif.type) {
                                            NotificationType.LIKE -> Icons.Filled.Favorite
                                            NotificationType.COMMENT, NotificationType.REPLY -> Icons.Filled.ModeComment
                                            NotificationType.SHARE -> Icons.Filled.Share
                                            NotificationType.MESSAGE -> Icons.Filled.Chat
                                            NotificationType.FRIEND_REQUEST, NotificationType.FRIEND_ACCEPT -> Icons.Filled.PersonAdd
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
    onTestNotification: () -> Unit = {},
    notificationSettings: Map<String, Boolean> = emptyMap(),
    onToggleNotificationSetting: (String) -> Unit = {},
    onTestLike: () -> Unit = {},
    onTestComment: () -> Unit = {},
    onTestShare: () -> Unit = {},
    onTestMessage: () -> Unit = {},
    blockedUserIds: Set<String> = emptySet(),
    onUnblockUser: (String) -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onBackClick: () -> Unit
) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = FadxAccentCoral,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Account Permanently?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "This action is permanent and cannot be undone. In accordance with Google Play data policy, all your profile data, posts, photos, friend connections, and chat messages will be permanently deleted from our servers.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FadxAccentCoral),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteAccountDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBlockedUsersDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedUsersDialog = false },
            title = { Text("Blocked Users (${blockedUserIds.size})", fontWeight = FontWeight.Bold) },
            text = {
                if (blockedUserIds.isEmpty()) {
                    Text("You have not blocked any users. When you block someone, they cannot see your posts or message you.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        blockedUserIds.forEach { userId ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("User ID: ${userId.take(8)}...", fontSize = 14.sp)
                                TextButton(onClick = { onUnblockUser(userId) }) {
                                    Text("Unblock", color = FadxPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedUsersDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

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

            // Firebase Auth & Cloud Firestore Integration Status
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
                                    Icons.Default.VerifiedUser,
                                    contentDescription = "Firebase Auth",
                                    tint = FadxPrimary
                                )
                                Text("Firebase Auth & Firestore", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1B5E20).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Connected 🟢",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Project: fadx-social-app",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Authentication: Unique Email & Password with Verification",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Backend Store: Cloud Firestore (users/{uid}) + Local Room DB",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            // Push Notifications & Alerts
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = FadxPrimary)
                            Text("Push Notifications & Alerts", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Text(
                            text = "Control which alerts pop up on your device status bar with sound & vibration.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Master Push Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Allow Push Notifications", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                Text("Master switch for all device notifications", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = notificationSettings["push_enabled"] ?: true,
                                onCheckedChange = { onToggleNotificationSetting("push_enabled") },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FadxPrimary)
                            )
                        }

                        // Likes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = FadxAccentCoral, modifier = Modifier.size(18.dp))
                                Text("Likes & Reactions", fontSize = 13.sp)
                            }
                            Switch(
                                checked = notificationSettings["likes"] ?: true,
                                onCheckedChange = { onToggleNotificationSetting("likes") },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FadxPrimary)
                            )
                        }

                        // Comments
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ModeComment, contentDescription = null, tint = FadxPrimary, modifier = Modifier.size(18.dp))
                                Text("Comments & Replies", fontSize = 13.sp)
                            }
                            Switch(
                                checked = notificationSettings["comments"] ?: true,
                                onCheckedChange = { onToggleNotificationSetting("comments") },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FadxPrimary)
                            )
                        }

                        // Shares
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                                Text("Post Shares & Reposts", fontSize = 13.sp)
                            }
                            Switch(
                                checked = notificationSettings["shares"] ?: true,
                                onCheckedChange = { onToggleNotificationSetting("shares") },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FadxPrimary)
                            )
                        }

                        // Messages
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = FadxSecondary, modifier = Modifier.size(18.dp))
                                Text("Direct Chat Messages", fontSize = 13.sp)
                            }
                            Switch(
                                checked = notificationSettings["messages"] ?: true,
                                onCheckedChange = { onToggleNotificationSetting("messages") },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FadxPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Test Alerts in Status Bar:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = onTestLike,
                                label = { Text("❤️ Like", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(
                                onClick = onTestComment,
                                label = { Text("💬 Comment", fontSize = 11.sp) },
                                modifier = Modifier.weight(1.1f)
                            )
                            AssistChip(
                                onClick = onTestShare,
                                label = { Text("↗️ Share", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(
                                onClick = onTestMessage,
                                label = { Text("✉️ Chat", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
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

            // Safety & Blocked Users (Google Play UGC compliance)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Safety & Interactions", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Blocked Accounts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${blockedUserIds.size} users currently blocked", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            OutlinedButton(
                                onClick = { showBlockedUsersDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Manage")
                            }
                        }
                    }
                }
            }

            // Danger Zone - Account Deletion (Google Play Data Policy compliance)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = FadxAccentCoral.copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = FadxAccentCoral)
                            Text("Account Deletion", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FadxAccentCoral)
                        }

                        Text(
                            text = "Permanently remove your Fadx profile, posts, messages, and all data from our cloud servers. This fulfills Google Play data deletion compliance.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { showDeleteAccountDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = FadxAccentCoral),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_account_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Account Permanently", fontWeight = FontWeight.Bold, color = Color.White)
                        }
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
