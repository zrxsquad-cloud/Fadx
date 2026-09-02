package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Post
import com.example.model.ReactionType
import com.example.model.User
import com.example.ui.components.AvatarImage
import com.example.ui.components.FadxBrandIcon
import com.example.ui.components.FadxLogo
import com.example.ui.components.PostCard
import com.example.ui.theme.FadxAccentAmber
import com.example.ui.theme.FadxAccentCoral
import com.example.ui.theme.FadxAccentGreen
import com.example.ui.theme.FadxPrimary
import com.example.ui.theme.FadxSecondary

@Composable
fun ProfileScreen(
    user: User,
    isCurrentUser: Boolean,
    userPosts: List<Post>,
    currentUserId: String,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: (() -> Unit)?,
    onStartChat: () -> Unit,
    onToggleFollow: () -> Unit,
    onReactionSelect: (Post, ReactionType) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onSaveClick: (Post) -> Unit,
    onReportClick: (Post) -> Unit,
    onDeleteClick: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Posts, 1: Photos, 2: About

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Cover Photo & Avatar Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // Cover Image
                AsyncImage(
                    model = user.coverUrl,
                    contentDescription = "Cover photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )

                // Back / Settings Top Bar Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (isCurrentUser) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }

                // Avatar overlapping cover
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                ) {
                    AvatarImage(
                        url = user.avatarUrl,
                        name = user.name,
                        size = 88.dp,
                        showOnlineIndicator = true,
                        isOnline = user.isOnline
                    )
                }
            }
        }

        // Profile Details & Metrics
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (user.isVerified) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Verified Badge",
                            tint = FadxPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "@${user.username}",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = user.bio,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata (Location, Website, Joined)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!user.location.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Text(user.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (!user.website.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Link, contentDescription = null, tint = FadxPrimary, modifier = Modifier.size(14.dp))
                            Text(user.website, fontSize = 12.sp, color = FadxPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text("Joined Fadx in 2024", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Social Counters (Followers, Following, Friends, Posts)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatItem(title = "Posts", value = userPosts.size.toString())
                        ProfileStatItem(title = "Followers", value = "${user.followersCount / 1000}k")
                        ProfileStatItem(title = "Following", value = user.followingCount.toString())
                        ProfileStatItem(title = "Friends", value = user.friendsCount.toString())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                if (isCurrentUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onEditProfileClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("edit_profile_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggleFollow,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Follow", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onStartChat,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Message", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Tab Selector (Posts, Photos, About)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = FadxPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Posts (${userPosts.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Media", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("About", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Tab Content
        if (selectedTab == 0) {
            items(userPosts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    currentUserId = currentUserId,
                    onReactionSelect = { reaction -> onReactionSelect(post, reaction) },
                    onCommentClick = { onCommentClick(post) },
                    onShareClick = { onShareClick(post) },
                    onSaveClick = { onSaveClick(post) },
                    onReportClick = { onReportClick(post) },
                    onDeleteClick = { onDeleteClick(post) },
                    onAuthorClick = {}
                )
            }
        } else if (selectedTab == 1) {
            item {
                val mediaList = userPosts.flatMap { it.mediaUrls }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Photos & Videos Gallery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mediaList.take(3).forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Gallery item",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Email: ${user.email}", fontSize = 13.sp)
                            Text("Work: Founder & Creative Director", fontSize = 13.sp)
                            Text("Education: Stanford University", fontSize = 13.sp)
                            Text("Relationship: Single", fontSize = 13.sp)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FadxBrandIcon(size = 38.dp, cornerRadius = 10.dp)
                            Column {
                                Text("Fadx Member", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Active creator in the Fadx community", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    currentUser: User,
    onSaveProfile: (name: String, bio: String, location: String, website: String, avatar: String?, cover: String?) -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf(currentUser.name) }
    var bio by remember { mutableStateOf(currentUser.bio) }
    var location by remember { mutableStateOf(currentUser.location ?: "") }
    var website by remember { mutableStateOf(currentUser.website ?: "") }
    var avatarUrl by remember { mutableStateOf(currentUser.avatarUrl) }
    var coverUrl by remember { mutableStateOf(currentUser.coverUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSaveProfile(name, bio, location, website, avatarUrl, coverUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar editor
            Box(contentAlignment = Alignment.BottomEnd) {
                AvatarImage(
                    url = avatarUrl,
                    name = name,
                    size = 90.dp
                )

                IconButton(
                    onClick = {
                        // Toggle sample avatar presets
                        avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=500&q=80"
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FadxPrimary)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }
    }
}
