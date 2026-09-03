package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.FadxAccentAmber
import com.example.ui.theme.FadxAccentCoral
import com.example.ui.theme.FadxAccentGreen
import com.example.ui.theme.FadxPrimary

@Composable
fun HomeScreen(
    currentUser: User,
    stories: List<Story>,
    posts: List<Post>,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Story) -> Unit,
    onCreatePostClick: () -> Unit,
    onReactionSelect: (Post, ReactionType) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onSaveClick: (Post) -> Unit,
    onReportClick: (Post) -> Unit,
    onDeleteClick: (Post) -> Unit,
    onAuthorClick: (User) -> Unit,
    onBlockAuthorClick: ((User) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Quick "What's on your mind?" composer box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarImage(
                            url = currentUser.avatarUrl,
                            name = currentUser.name,
                            size = 42.dp
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { onCreatePostClick() }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "What's on your mind, ${currentUser.name.split(" ").first()}?",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Shortcut actions: Photo, Video, Feeling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCreatePostClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Photo",
                                tint = FadxAccentGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Photo", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCreatePostClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video",
                                tint = FadxAccentCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Video", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCreatePostClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEmotions,
                                contentDescription = "Feeling",
                                tint = FadxAccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Feeling", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Stories Section
        item {
            StoriesRow(
                currentUser = currentUser,
                stories = stories,
                onAddStoryClick = onAddStoryClick,
                onStoryClick = onStoryClick
            )
        }

        // Feed Posts
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                currentUserId = currentUser.id,
                onReactionSelect = { reaction -> onReactionSelect(post, reaction) },
                onCommentClick = { onCommentClick(post) },
                onShareClick = { onShareClick(post) },
                onSaveClick = { onSaveClick(post) },
                onReportClick = { onReportClick(post) },
                onDeleteClick = { onDeleteClick(post) },
                onAuthorClick = onAuthorClick,
                onBlockAuthorClick = onBlockAuthorClick
            )
        }
    }
}
