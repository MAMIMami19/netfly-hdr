package com.nuvio.tv.ui.netflyhdr.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.ui.netflyhdr.NetflyHdrRed

/**
 * The Netfly ListScreen style (§10): background + 0.22-alpha backdrop image +
 * 8×56 red vertical mark + title + subtitle + rows with a white pill on focus
 * + divider + bottom detail line.
 *
 * Surfaces and text come from MaterialTheme.colorScheme (theme builder owns
 * the palette); only the red mark uses the Netfly accent. The white focus
 * pill uses the theme's inverse surface so it stays readable in any theme.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrListScreen(
    title: String,
    subtitle: String,
    backdropUrl: String?,
    modifier: Modifier = Modifier,
    detailLine: String? = null,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.22f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 8×56 red vertical mark (§10).
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(56.dp)
                        .background(NetflyHdrRed, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                headerActions()
            }
            Spacer(modifier = Modifier.height(24.dp))
            content()
            if (!detailLine.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = detailLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * One ListScreen row: 64dp tall, full width. On focus the row becomes a
 * white pill (theme inverse surface) with dark text; otherwise it is
 * transparent with a divider underneath.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrListRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (BoxScope.() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val pillShape = remember { RoundedCornerShape(32.dp) }
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    color = if (isFocused) MaterialTheme.colorScheme.inverseSurface
                    else androidx.compose.ui.graphics.Color.Transparent,
                    shape = pillShape
                )
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        leading()
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isFocused) MaterialTheme.colorScheme.inverseOnSurface
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFocused) MaterialTheme.colorScheme.inverseOnSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    trailing()
                }
            }
        }
        // Divider (§10: #2A2A36 — rendered from the theme outline).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 20.dp)
                .background(MaterialTheme.colorScheme.border.copy(alpha = 0.4f))
        )
    }
}
