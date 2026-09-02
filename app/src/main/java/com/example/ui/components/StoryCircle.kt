package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Story
import com.example.model.User
import com.example.ui.theme.FadxPrimary
import com.example.ui.theme.FadxSecondary

@Composable
fun StoriesRow(
    currentUser: User,
    stories: List<Story>,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Story) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Your Story" item with Add button
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable { onAddStoryClick() }
                ) {
                    Box(
                        modifier = Modifier.size(58.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarImage(
                            url = currentUser.avatarUrl,
                            name = currentUser.name,
                            size = 56.dp
                        )

                        // Plus badge
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(FadxPrimary, FadxSecondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Story",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Your Story",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Other User Stories
            items(stories.filter { it.author.id != currentUser.id }) { story ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable { onStoryClick(story) }
                ) {
                    AvatarImage(
                        url = story.author.avatarUrl,
                        name = story.author.name,
                        size = 58.dp,
                        hasStoryRing = true,
                        isStoryViewed = story.isViewedByMe
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = story.author.name.split(" ").firstOrNull() ?: story.author.name,
                        fontSize = 11.sp,
                        fontWeight = if (!story.isViewedByMe) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
