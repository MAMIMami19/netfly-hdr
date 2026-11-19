package com.nuvio.tv.ui.netflyhdr.themes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Original "Netfly HDR" identity, drawn in code — no copied logos.
 *
 * The mark is a red rounded-square play badge followed by the word NETFLY in
 * heavy wide-tracked capitals and a bordered HDR chip. It deliberately does
 * NOT imitate the arched Netflix-style lettering of the Roku original.
 */

/** The red play badge: a rounded square with a white play triangle. */
@Composable
fun NetflyPlayBadge(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(NETFLY_RED, RoundedCornerShape(size * 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.5f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.28f, h * 0.12f)
                lineTo(w * 0.88f, h * 0.5f)
                lineTo(w * 0.28f, h * 0.88f)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

/**
 * The drawn Netfly HDR wordmark: play badge + NETFLY in heavy capitals +
 * bordered HDR chip. Scales with [fontSize].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyWordmark(
    fontSize: TextUnit = 44.sp,
    modifier: Modifier = Modifier
) {
    val description = stringResource(R.string.netfly_wordmark_description)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NetflyPlayBadge(size = (fontSize.value * 1.05f).dp)
        Text(
            text = "NETFLY",
            style = MaterialTheme.typography.displayMedium,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = (fontSize.value * 0.06f).sp
        )
        Box(
            modifier = Modifier
                .border(2.dp, NETFLY_RED, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "HDR",
                style = MaterialTheme.typography.titleMedium,
                fontSize = (fontSize.value * 0.42f).sp,
                fontWeight = FontWeight.Black,
                color = NETFLY_RED,
                letterSpacing = (fontSize.value * 0.03f).sp
            )
        }
    }
}

/**
 * Original Netfly splash screen: black #0B080B with a red glow in the top
 * corners, the wordmark centred in the upper third, and a loading indicator.
 * Shown on startup when the active profile has no custom background or color.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflySplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B080B))
    ) {
        // Red glow in both top corners (size-aware, drawn in one layer).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val radius = size.minDimension * 0.9f
                    val leftGlow = Brush.radialGradient(
                        colors = listOf(
                            NETFLY_RED.copy(alpha = 0.34f),
                            NETFLY_RED.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = radius
                    )
                    val rightGlow = Brush.radialGradient(
                        colors = listOf(
                            NETFLY_RED.copy(alpha = 0.30f),
                            NETFLY_RED.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width, 0f),
                        radius = radius
                    )
                    onDrawBehind {
                        drawRect(leftGlow)
                        drawRect(rightGlow)
                    }
                }
        )
        // Gentle bottom fade so the loader sits on solid black.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.55f to Color.Transparent,
                            1f to Color(0xFF0B080B)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NetflyWordmark(fontSize = 54.sp)
            Spacer(modifier = Modifier.height(NuvioTheme.spacing.xxl))
            LoadingIndicator(modifier = Modifier.size(NuvioTheme.spacing.xxxl))
        }
    }
}
