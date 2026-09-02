package com.example.ui.screens.hubs

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.components.AvatarImage
import com.example.ui.theme.*

// ----------------- GROUPS HUB -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    groups: List<Group>,
    onGroupClick: (Group) -> Unit,
    onCreateGroupClick: () -> Unit,
    onToggleJoin: (Group) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Communities & Groups", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = onCreateGroupClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create", fontWeight = FontWeight.Bold)
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
            items(groups) { group ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.clickable { onGroupClick(group) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = group.coverUrl,
                            contentDescription = group.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "${if (group.isPrivate) "Private" else "Public"} • ${group.category} • ${group.membersCount} members",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { onToggleJoin(group) },
                                    colors = if (group.isJoined) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (group.isJoined) "Joined" else "Join")
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(group.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

// ----------------- PAGES HUB -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagesScreen(
    pages: List<Page>,
    onPageClick: (Page) -> Unit,
    onCreatePageClick: () -> Unit,
    onToggleFollow: (Page) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pages & Creators", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = onCreatePageClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create", fontWeight = FontWeight.Bold)
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
            items(pages) { page ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable { onPageClick(page) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarImage(url = page.avatarUrl, name = page.name, size = 56.dp)

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(page.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FadxPrimary, modifier = Modifier.size(16.dp))
                            }
                            Text("${page.category.label} • ${page.followersCount} followers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(page.bio, fontSize = 13.sp, maxLines = 2, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { onToggleFollow(page) },
                            colors = if (page.isFollowed) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (page.isFollowed) "Following" else "Follow")
                        }
                    }
                }
            }
        }
    }
}

// ----------------- MARKETPLACE HUB -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    products: List<Product>,
    onCreateListingClick: () -> Unit,
    onProductClick: (Product) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marketplace", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = onCreateListingClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sell Item", fontWeight = FontWeight.Bold)
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Browse Nearby Listings (${products.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 800.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products) { product ->
                            ProductGridCard(product = product, onClick = { onProductClick(product) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(product: Product, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = product.images.firstOrNull() ?: "",
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "$${product.price.toInt()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = FadxPrimary
                )
                Text(
                    text = product.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${product.location} • ${product.category.label}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ----------------- EVENTS HUB -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    events: List<EventItem>,
    onCreateEventClick: () -> Unit,
    onToggleGoing: (EventItem) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events & Meetups", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = onCreateEventClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Host Event", fontWeight = FontWeight.Bold)
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
            items(events) { event ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = event.coverUrl,
                            contentDescription = event.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = event.date,
                                    color = FadxAccentCoral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${event.goingCount} going • ${event.interestedCount} interested",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(event.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "${event.location} • Hosted by ${event.organizer.name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onToggleGoing(event) },
                                    colors = if (event.isGoing) ButtonDefaults.buttonColors(containerColor = FadxAccentGreen) else ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (event.isGoing) "Going 🎉" else "Join (Going)")
                                }

                                OutlinedButton(
                                    onClick = {},
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.StarOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Interested")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
