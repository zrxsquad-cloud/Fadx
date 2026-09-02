package com.example.ui.screens.videos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.User
import com.example.model.VideoItem
import com.example.ui.components.AvatarImage
import com.example.ui.theme.FadxAccentCoral
import com.example.ui.theme.FadxPrimary
import com.example.ui.theme.FadxSecondary

@Composable
fun VideosScreen(
    videos: List<VideoItem>,
    videoTab: Int, // 0: Short Reels, 1: Long Feed
    onVideoTabChange: (Int) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onToggleLike: (VideoItem) -> Unit,
    onAuthorClick: (User) -> Unit,
    onShareVideo: (VideoItem) -> Unit,
    onCommentVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Selector Header: "Shorts" vs "Feed"
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                TabRow(
                    selectedTabIndex = videoTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = FadxPrimary
                ) {
                    Tab(
                        selected = videoTab == 0,
                        onClick = { onVideoTabChange(0) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Shorts (Reels)", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = videoTab == 1,
                        onClick = { onVideoTabChange(1) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Video Feed", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Sub-category filters for Video Feed
                if (videoTab == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Recommended", "Trending", "Following", "Saved").forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategoryChange(cat) },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FadxPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (videoTab == 0) {
            // Full Screen Vertical Shorts / Reels Viewer
            ShortsReelViewer(
                videos = videos.filter { it.isShort },
                onToggleLike = onToggleLike,
                onAuthorClick = onAuthorClick,
                onShare = onShareVideo,
                onComment = onCommentVideo
            )
        } else {
            // Video Feed List (Longer videos + Recommended/Trending)
            val filteredVideos = if (selectedCategory == "Trending") {
                videos.sortedByDescending { it.likesCount }
            } else if (selectedCategory == "Saved") {
                videos.filter { it.isSaved }
            } else {
                videos
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredVideos, key = { it.id }) { video ->
                    VideoFeedCard(
                        video = video,
                        onToggleLike = { onToggleLike(video) },
                        onAuthorClick = onAuthorClick,
                        onShare = { onShareVideo(video) },
                        onComment = { onCommentVideo(video) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShortsReelViewer(
    videos: List<VideoItem>,
    onToggleLike: (VideoItem) -> Unit,
    onAuthorClick: (User) -> Unit,
    onShare: (VideoItem) -> Unit,
    onComment: (VideoItem) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentVideo = videos.getOrNull(currentIndex) ?: return
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Thumbnail & Simulated Video Frame
        AsyncImage(
            model = currentVideo.thumbnailUrl,
            contentDescription = currentVideo.title,
            modifier = Modifier
                .fillMaxSize()
                .clickable { isPlaying = !isPlaying },
            contentScale = ContentScale.Crop
        )

        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Play/Pause Center Indicator
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Top Navigation Controls (Swipe Up/Down Simulation Buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = FadxSecondary, modifier = Modifier.size(16.dp))
                    Text("Reel ${currentIndex + 1}/${videos.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Prev Reel
                IconButton(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Reel", tint = Color.White)
                }

                // Next Reel
                IconButton(
                    onClick = { if (currentIndex < videos.size - 1) currentIndex++ },
                    enabled = currentIndex < videos.size - 1,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Reel", tint = Color.White)
                }

                // Mute/Unmute
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Audio toggle",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Left Video Meta (Author, Title, Sound Track)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 88.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.clickable { onAuthorClick(currentVideo.author) }
            ) {
                AvatarImage(
                    url = currentVideo.author.avatarUrl,
                    name = currentVideo.author.name,
                    size = 40.dp
                )
                Text(
                    text = "@${currentVideo.author.username}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FadxPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "Follow",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentVideo.description,
                color = Color.White,
                fontSize = 13.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(
                    text = currentVideo.soundTrackTitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        // Right Side Floating Interaction Column (Like, Comment, Share, Save)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { onToggleLike(currentVideo) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (currentVideo.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (currentVideo.isLiked) FadxAccentCoral else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "${currentVideo.likesCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Comment
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { onComment(currentVideo) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Comment",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${currentVideo.commentsCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Share
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { onShare(currentVideo) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${currentVideo.sharesCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VideoFeedCard(
    video: VideoItem,
    onToggleLike: () -> Unit,
    onAuthorClick: (User) -> Unit,
    onShare: () -> Unit,
    onComment: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video Thumbnail & Player container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color.Black)
                    .clickable { isPlaying = !isPlaying }
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Duration badge
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = video.duration,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Play Button Center
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            // Video Meta & Author details
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = video.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { onAuthorClick(video.author) }
                    ) {
                        AvatarImage(
                            url = video.author.avatarUrl,
                            name = video.author.name,
                            size = 32.dp
                        )
                        Text(
                            text = video.author.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { onToggleLike() }
                        ) {
                            Icon(
                                imageVector = if (video.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (video.isLiked) FadxAccentCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${video.likesCount}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { onComment() }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ModeComment,
                                contentDescription = "Comment",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${video.commentsCount}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
