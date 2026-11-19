package com.nuvio.tv.ui.netflyhdr.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Home skeleton loading state (design spec §4): pulsing grey cards mimicking
 * the hero and the rows — NEVER a black screen while loading.
 *
 * Pulse: alpha 1.0 → 0.45 over 1.2s, repeating. Card greys are the spec's
 * #1C1C20 / #1A1A1E. With reduce-motion on, the skeleton renders statically
 * (no pulse).
 */
@Composable
fun NetflyHomeSkeleton(
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalNetflyHomePrefs.current.reduceMotion
    val transition = rememberInfiniteTransition(label = "netflyHomeSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "netflySkeletonPulse"
    )
    val alpha = if (reduceMotion) 0.8f else pulse

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background)
            .padding(top = 32.dp, start = 76.dp, end = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Hero block.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1C20))
        )
        // Two mimicked rows: title bar + poster cards.
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(18.dp)
                        .alpha(alpha)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1A1A1E))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    repeat(6) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(150.dp)
                                .alpha(alpha)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1C1C20))
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
