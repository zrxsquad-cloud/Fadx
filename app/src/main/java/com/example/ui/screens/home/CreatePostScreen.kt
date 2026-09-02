package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PostVisibility
import com.example.model.User
import com.example.ui.components.AvatarImage
import com.example.ui.theme.FadxAccentAmber
import com.example.ui.theme.FadxAccentCoral
import com.example.ui.theme.FadxAccentGreen
import com.example.ui.theme.FadxPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    currentUser: User,
    onBackClick: () -> Unit,
    onPostSubmit: (text: String, mediaUrls: List<String>, visibility: PostVisibility, location: String?) -> Unit
) {
    var postText by remember { mutableStateOf("") }
    var selectedVisibility by remember { mutableStateOf(PostVisibility.PUBLIC) }
    var locationInput by remember { mutableStateOf("") }
    var showLocationField by remember { mutableStateOf(false) }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showVisibilityMenu by remember { mutableStateOf(false) }

    val sampleMediaPresets = listOf(
        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1000&q=80",
        "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1000&q=80",
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1000&q=80",
        "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=1000&q=80"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Post",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (postText.isNotBlank() || attachedImages.isNotEmpty()) {
                                onPostSubmit(
                                    postText,
                                    attachedImages,
                                    selectedVisibility,
                                    if (showLocationField && locationInput.isNotBlank()) locationInput else null
                                )
                            }
                        },
                        enabled = postText.isNotBlank() || attachedImages.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = FadxPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("submit_post_button")
                    ) {
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // User Header & Privacy Selector Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarImage(
                    url = currentUser.avatarUrl,
                    name = currentUser.name,
                    size = 48.dp
                )

                Column {
                    Text(
                        text = currentUser.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Privacy audience selector button
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { showVisibilityMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (selectedVisibility) {
                                        PostVisibility.PUBLIC -> Icons.Outlined.Public
                                        PostVisibility.FRIENDS -> Icons.Outlined.Group
                                        PostVisibility.ONLY_ME -> Icons.Outlined.Lock
                                        PostVisibility.CUSTOM -> Icons.Outlined.Tune
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = selectedVisibility.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showVisibilityMenu,
                            onDismissRequest = { showVisibilityMenu = false }
                        ) {
                            PostVisibility.values().forEach { vis ->
                                DropdownMenuItem(
                                    text = { Text(vis.label) },
                                    onClick = {
                                        selectedVisibility = vis
                                        showVisibilityMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (vis) {
                                                PostVisibility.PUBLIC -> Icons.Outlined.Public
                                                PostVisibility.FRIENDS -> Icons.Outlined.Group
                                                PostVisibility.ONLY_ME -> Icons.Outlined.Lock
                                                PostVisibility.CUSTOM -> Icons.Outlined.Tune
                                            },
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Post Text Area
            OutlinedTextField(
                value = postText,
                onValueChange = { postText = it },
                placeholder = {
                    Text(
                        text = "What would you like to share today? Add photos, tag friends, or share your thoughts...",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .testTag("create_post_text_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            // Location Input row if active
            if (showLocationField) {
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Location") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = FadxAccentCoral) },
                    trailingIcon = {
                        IconButton(onClick = { showLocationField = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove location")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Attached Media Preview
            if (attachedImages.isNotEmpty()) {
                Text(
                    text = "Attached Photos (${attachedImages.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(attachedImages) { url ->
                        Box(modifier = Modifier.size(100.dp)) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Attached Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(
                                onClick = { attachedImages = attachedImages - url },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Emoji Inserter
            Text(
                text = "Quick Emojis",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("✨", "🚀", "🔥", "💡", "🌿", "🎉", "💙", "😎").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { postText += " $emoji " }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attachment Tool Options (Add Photos, Location, Feeling, Tag Friends)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Add to your post",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IconButton(
                            onClick = {
                                val nextPreset = sampleMediaPresets.firstOrNull { it !in attachedImages } ?: sampleMediaPresets.first()
                                attachedImages = attachedImages + nextPreset
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Photo",
                                tint = FadxAccentGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = { showLocationField = !showLocationField }
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Add Location",
                                tint = FadxAccentCoral,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = { postText += " feeling inspired 💡" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEmotions,
                                contentDescription = "Add Feeling",
                                tint = FadxAccentAmber,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = { postText += " with @elenarostova @marcuschen " }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Tag Friends",
                                tint = FadxPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
