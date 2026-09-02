package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReactionType

@Composable
fun ReactionPickerPopup(
    visible: Boolean,
    onReactionSelected: (ReactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val reactions = listOf(
        ReactionType.LIKE,
        ReactionType.LOVE,
        ReactionType.HAHA,
        ReactionType.WOW,
        ReactionType.SAD,
        ReactionType.ANGRY
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.shadow(12.dp, RoundedCornerShape(32.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                reactions.forEach { reaction ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onReactionSelected(reaction) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reaction.emoji,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}
