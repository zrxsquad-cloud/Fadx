package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.FadxAccentGreen
import com.example.ui.theme.FadxPrimary
import com.example.ui.theme.FadxSecondary

@Composable
fun AvatarImage(
    url: String?,
    name: String,
    size: Dp = 44.dp,
    showOnlineIndicator: Boolean = false,
    isOnline: Boolean = false,
    hasStoryRing: Boolean = false,
    isStoryViewed: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val ringBrush = when {
        hasStoryRing && !isStoryViewed -> Brush.linearGradient(
            listOf(FadxPrimary, FadxSecondary)
        )
        hasStoryRing -> Brush.linearGradient(
            listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f))
        )
        else -> null
    }

    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val imageModifier = Modifier
            .fillMaxSize()
            .then(
                if (ringBrush != null) {
                    Modifier
                        .border(2.5.dp, ringBrush, CircleShape)
                        .padding(3.dp)
                } else Modifier
            )
            .clip(CircleShape)

        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = name,
                modifier = imageModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            // Initial placeholder with brand gradient
            val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
            Box(
                modifier = imageModifier.background(
                    Brush.linearGradient(listOf(FadxPrimary, Color(0xFF4834D4)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials.ifBlank { "F" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }

        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isOnline) FadxAccentGreen else Color.Gray)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
