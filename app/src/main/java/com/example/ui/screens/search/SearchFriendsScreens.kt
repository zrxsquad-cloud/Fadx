package com.example.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.AvatarImage
import com.example.ui.theme.FadxAccentCoral
import com.example.ui.theme.FadxPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    searchResults: Map<String, List<Any>>,
    onUserClick: (User) -> Unit,
    onGroupClick: (Group) -> Unit,
    onPageClick: (Page) -> Unit,
    onBackClick: () -> Unit
) {
    val categories = listOf("All", "People", "Posts", "Videos", "Groups", "Pages", "Hashtags")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search on Fadx...", fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            focusedBorderColor = FadxPrimary
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = (selectedCategory == cat),
                        onClick = { onCategorySelect(cat) },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FadxPrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            if (query.isBlank()) {
                // Recent Searches & Trending Topics
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Trending Topics & Hashtags",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        "#AIInnovation" to "24.5k posts",
                        "#FadxCreators" to "18.2k posts",
                        "#ModernArchitecture" to "12.8k posts",
                        "#TechSummit2026" to "9.4k posts",
                        "#HealthyLiving" to "8.1k posts"
                    ).forEach { (tag, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQueryChange(tag.replace("#", "")) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Tag, contentDescription = null, tint = FadxPrimary)
                                Column {
                                    Text(tag, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(count, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                // Search Results
                val people = (searchResults["people"] as? List<*>)?.filterIsInstance<User>() ?: emptyList()
                val groups = (searchResults["groups"] as? List<*>)?.filterIsInstance<Group>() ?: emptyList()
                val pages = (searchResults["pages"] as? List<*>)?.filterIsInstance<Page>() ?: emptyList()
                val posts = (searchResults["posts"] as? List<*>)?.filterIsInstance<Post>() ?: emptyList()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (people.isNotEmpty() && (selectedCategory == "All" || selectedCategory == "People")) {
                        item {
                            Text("People", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        items(people) { user ->
                            UserSearchRow(user = user, onClick = { onUserClick(user) })
                        }
                    }

                    if (groups.isNotEmpty() && (selectedCategory == "All" || selectedCategory == "Groups")) {
                        item {
                            Text("Groups", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        items(groups) { group ->
                            GroupSearchRow(group = group, onClick = { onGroupClick(group) })
                        }
                    }

                    if (pages.isNotEmpty() && (selectedCategory == "All" || selectedCategory == "Pages")) {
                        item {
                            Text("Pages", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        items(pages) { page ->
                            PageSearchRow(page = page, onClick = { onPageClick(page) })
                        }
                    }

                    if (people.isEmpty() && groups.isEmpty() && pages.isEmpty() && posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No results found for \"$query\"",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchRow(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarImage(url = user.avatarUrl, name = user.name, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (user.isVerified) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FadxPrimary, modifier = Modifier.size(14.dp))
                    }
                }
                Text("@${user.username} • ${user.bio}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun GroupSearchRow(group: Group, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarImage(url = group.avatarUrl, name = group.name, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${if (group.isPrivate) "Private" else "Public"} Group • ${group.membersCount} members", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PageSearchRow(page: Page, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarImage(url = page.avatarUrl, name = page.name, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(page.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${page.category.label} • ${page.followersCount} followers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    requests: List<FriendRequest>,
    friends: List<User>,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onUserClick: (User) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Requests, 1: Friends

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends & Connections", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FadxPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Requests (${requests.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("All Friends (${friends.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                if (requests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pending friend requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(requests) { req ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AvatarImage(
                                        url = req.user.avatarUrl,
                                        name = req.user.name,
                                        size = 50.dp,
                                        onClick = { onUserClick(req.user) }
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(req.user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${req.mutualFriendsCount} mutual friends • ${req.timestamp}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onAcceptRequest(req.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("Confirm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { onDeclineRequest(req.id) },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("Delete", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(friends) { friend ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.clickable { onUserClick(friend) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AvatarImage(
                                    url = friend.avatarUrl,
                                    name = friend.name,
                                    size = 46.dp,
                                    showOnlineIndicator = true,
                                    isOnline = friend.isOnline
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("@${friend.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                IconButton(onClick = { onRemoveFriend(friend.id) }) {
                                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = FadxAccentCoral)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
