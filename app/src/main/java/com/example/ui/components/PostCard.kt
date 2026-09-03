package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    onReactionSelect: (ReactionType) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAuthorClick: (User) -> Unit,
    onBlockAuthorClick: ((User) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var selectedCarouselIndex by remember { mutableIntStateOf(0) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Author info, Location, Privacy, Options Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAuthorClick(post.author) }
                ) {
                    AvatarImage(
                        url = post.author.avatarUrl,
                        name = post.author.name,
                        size = 46.dp
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = post.author.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (post.author.isVerified) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = FadxPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "@${post.author.username} • ${post.timestamp}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Icon(
                                imageVector = when (post.visibility) {
                                    PostVisibility.PUBLIC -> Icons.Outlined.Public
                                    PostVisibility.FRIENDS -> Icons.Outlined.Group
                                    PostVisibility.ONLY_ME -> Icons.Outlined.Lock
                                    PostVisibility.CUSTOM -> Icons.Outlined.Tune
                                },
                                contentDescription = post.visibility.label,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Options dropdown button
                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Post Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (post.isSaved) "Remove from Saved" else "Save Post") },
                            onClick = {
                                showOptionsMenu = false
                                onSaveClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Report Post") },
                            onClick = {
                                showOptionsMenu = false
                                onReportClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Report,
                                    contentDescription = null,
                                    tint = FadxAccentCoral
                                )
                            }
                        )

                        if (post.author.id != currentUserId && onBlockAuthorClick != null) {
                            DropdownMenuItem(
                                text = { Text("Block @${post.author.username}", color = FadxAccentCoral) },
                                onClick = {
                                    showOptionsMenu = false
                                    onBlockAuthorClick(post.author)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = FadxAccentCoral
                                    )
                                }
                            )
                        }

                        if (post.author.id == currentUserId) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = FadxAccentCoral) },
                                onClick = {
                                    showOptionsMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = FadxAccentCoral
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Location Tag if available
            if (!post.locationTag.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Location",
                        tint = FadxSecondaryDark,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = post.locationTag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FadxSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post Content Text
            if (post.text.isNotBlank()) {
                Text(
                    text = post.text,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Media Area (Single Image or Carousel)
            if (post.mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                if (post.mediaUrls.size == 1) {
                    AsyncImage(
                        model = post.mediaUrls.first(),
                        contentDescription = "Post Media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 340.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Carousel
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(post.mediaUrls) { index, url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Carousel item $index",
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Carousel Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            post.mediaUrls.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(if (index == selectedCarouselIndex) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == selectedCarouselIndex) FadxPrimary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reaction & Engagement Stats Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Reactions Preview
                if (post.totalReactions > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val reactionIcons = post.reactionsCount.keys.take(3).map { it.emoji }.joinToString("")
                        Text(text = reactionIcons, fontSize = 14.sp)
                        Text(
                            text = post.totalReactions.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Comments & Shares count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.commentsCount > 0) {
                        Text(
                            text = "${post.commentsCount} comments",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (post.sharesCount > 0) {
                        Text(
                            text = "${post.sharesCount} shares",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Bottom Action Bar: Like / React, Comment, Share, Save
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Like / Reaction Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (post.userReaction == ReactionType.NONE) {
                                    showReactionPicker = !showReactionPicker
                                } else {
                                    onReactionSelect(ReactionType.NONE)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (post.userReaction != ReactionType.NONE) {
                            Text(text = post.userReaction.emoji, fontSize = 18.sp)
                            Text(
                                text = post.userReaction.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (post.userReaction) {
                                    ReactionType.LOVE -> ReactionLove
                                    ReactionType.HAHA -> ReactionHaha
                                    ReactionType.WOW -> ReactionWow
                                    ReactionType.SAD -> ReactionSad
                                    ReactionType.ANGRY -> ReactionAngry
                                    else -> FadxPrimary
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "React",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Comment Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCommentClick() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ModeComment,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Comment",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onShareClick() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Share",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Save Button
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (post.isSaved) FadxPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Popup Reaction Picker Floating above the like button
                ReactionPickerPopup(
                    visible = showReactionPicker,
                    onReactionSelected = { reaction ->
                        showReactionPicker = false
                        onReactionSelect(reaction)
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = (-52).dp)
                )
            }
        }
    }
}
