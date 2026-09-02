package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.ui.theme.FadxPrimary
import com.example.ui.theme.FadxSecondary

data class ShareAction(val title: String, val icon: ImageVector, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    friends: List<User>,
    onDismiss: () -> Unit,
    onSendDirect: (User) -> Unit,
    onCopyLink: () -> Unit,
    onShareExternal: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Share to",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Direct Send to Friends
            Text(
                text = "Send via Fadx Chat",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(friends) { friend ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clickable {
                                onSendDirect(friend)
                                onDismiss()
                            }
                    ) {
                        AvatarImage(
                            url = friend.avatarUrl,
                            name = friend.name,
                            size = 50.dp,
                            showOnlineIndicator = true,
                            isOnline = friend.isOnline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = friend.name.split(" ").firstOrNull() ?: friend.name,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Action Pills (Copy Link, External Share, Share to Group, QR Code)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ShareActionButton(
                    title = "Copy Link",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        onCopyLink()
                        onDismiss()
                    }
                )

                ShareActionButton(
                    title = "External App",
                    icon = Icons.Default.Share,
                    onClick = {
                        onShareExternal()
                        onDismiss()
                    }
                )

                ShareActionButton(
                    title = "To Groups",
                    icon = Icons.Default.Groups,
                    onClick = {
                        onShareExternal()
                        onDismiss()
                    }
                )

                ShareActionButton(
                    title = "QR Code",
                    icon = Icons.Default.QrCode,
                    onClick = {
                        onShareExternal()
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ShareActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = FadxPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
