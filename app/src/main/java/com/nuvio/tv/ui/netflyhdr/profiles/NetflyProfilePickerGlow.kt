package com.nuvio.tv.ui.netflyhdr.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.tv.ui.netflyhdr.NetflyRed

/**
 * Netfly red background glow for the "Who's watching?" picker (design §11:
 * logo + background glow at 0.55 alpha).
 *
 * A radial gradient peaking at 0.55 alpha in the center and fading to
 * transparent, placed behind the wordmark. The base picker already supplies
 * the centered title, subtitle, profile row and hint; this is the one
 * Netfly visual the base screen was missing.
 */
@Composable
fun NetflyProfilePickerGlow(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(640.dp)
            .background(
                brush = Brush.radialGradient(
                    0f to NetflyRed.copy(alpha = 0.55f),
                    0.45f to NetflyRed.copy(alpha = 0.22f),
                    1f to Color.Transparent
                )
            )
    )
}
