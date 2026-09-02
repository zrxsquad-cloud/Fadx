package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FadxAccentCoral
import com.example.ui.theme.FadxPrimary
import com.example.ui.theme.FadxSecondary

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isPrimaryAction: Boolean = false,
    val badgeCount: Int = 0
)

@Composable
fun FadxBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    notificationsCount: Int = 2
) {
    val items = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("Videos", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline),
        NavItem("Create", Icons.Filled.Add, Icons.Filled.Add, isPrimaryAction = true),
        NavItem("Alerts", Icons.Filled.Notifications, Icons.Outlined.NotificationsNone, badgeCount = notificationsCount),
        NavItem("Menu", Icons.Filled.Menu, Icons.Outlined.Menu)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedTab == index

                if (item.isPrimaryAction) {
                    // Center Floating Add/Create Action Button
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(FadxPrimary, FadxSecondary)
                                )
                            )
                            .clickable { onTabSelected(index) }
                            .testTag("nav_create_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create Post",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    // Standard Nav Item with active indicator pill
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onTabSelected(index) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("nav_tab_$index"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = if (isSelected) FadxPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )

                            if (item.badgeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(FadxAccentCoral),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.badgeCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) FadxPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
