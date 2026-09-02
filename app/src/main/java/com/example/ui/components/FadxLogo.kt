package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.FadxPrimary
import com.example.ui.theme.FadxSecondary

/**
 * Official Fadx Brand Icon showing the stylized 'F' symbol with blue-to-purple gradient.
 */
@Composable
fun FadxBrandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = size * 0.26f
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    listOf(FadxPrimary, FadxSecondary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.fadx_app_icon_1788355035715),
            contentDescription = "Fadx Logo",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius)),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Official Fadx Logo Composable with the stylized F symbol and the Fadx wordmark.
 */
@Composable
fun FadxLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    textSize: TextUnit = 24.sp,
    showWordmark: Boolean = true,
    showTagline: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    taglineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((iconSize.value * 0.25f).dp)
    ) {
        FadxBrandIcon(
            size = iconSize,
            cornerRadius = (iconSize.value * 0.26f).dp
        )

        if (showWordmark) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "fadx",
                    fontSize = textSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                    color = textColor,
                    fontFamily = FontFamily.SansSerif
                )
                if (showTagline) {
                    Text(
                        text = "Connect • Create • Inspire",
                        fontSize = (textSize.value * 0.42f).sp,
                        fontWeight = FontWeight.Medium,
                        color = taglineColor
                    )
                }
            }
        }
    }
}

/**
 * Animated Loading experience with the official Fadx logo.
 */
@Composable
fun FadxLoadingIndicator(
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    message: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fadx_loading_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fadx_pulse_scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .scale(scale)
        ) {
            FadxBrandIcon(
                size = iconSize,
                cornerRadius = (iconSize.value * 0.26f).dp
            )
        }

        if (message != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
