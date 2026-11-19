package com.nuvio.tv.ui.netflyhdr.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Shared building blocks for the Netfly dialogs (Theme Manager, Home
 * settings). All focus handling is TV-native: DPAD moves between Cards, OK
 * activates the focused one, BACK dismisses.
 */

/**
 * Full-screen dialog shell: 92% dim scrim with a centered panel sized to the
 * spec's 1760×980 on a 1920×1080 canvas (91.7% × 90.7% of the screen, so it
 * scales with density).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyDialogShell(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.917f)
                    .fillMaxHeight(0.907f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NuvioTheme.colors.BackgroundElevated)
                    .border(
                        width = 1.dp,
                        color = NuvioTheme.colors.Border,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                content = content
            )
        }
    }
}

/** Dialog header: large title, red label line, small hint line. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyDialogHeader(
    title: String,
    redLabel: String,
    hint: String? = null
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = NuvioTheme.colors.TextPrimary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = redLabel,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = NETFLY_RED,
        letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing
    )
    if (hint != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = NuvioTheme.colors.TextTertiary
        )
    }
}

/** Small caps section label inside a Netfly dialog. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflySectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = NuvioTheme.colors.TextSecondary
    )
}

/** Focusable toggle row: title + subtitle on the left, check disc on the right. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.BackgroundCard
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(2.dp),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        scale = CardDefaults.scale(focusedScale = 1.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) {
                        NuvioTheme.colors.TextPrimary
                    } else {
                        NuvioTheme.colors.TextSecondary
                    }
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioTheme.colors.TextTertiary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) NuvioTheme.colors.Secondary else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (checked) {
                            NuvioTheme.colors.Secondary
                        } else {
                            NuvioTheme.colors.Border
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NuvioTheme.colors.OnSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Focusable option pill (dark/light toggle, hero mode, titles-per-row…).
 * Selected pills get the accent border; focused pills get the focus ring.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyOptionPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = if (selected) {
                NuvioTheme.colors.FocusBackground
            } else {
                NuvioTheme.colors.BackgroundCard
            },
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = CardDefaults.border(
            border = if (selected) {
                Border(
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        NuvioTheme.colors.Secondary
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            } else {
                Border.None
            },
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(2.dp),
                shape = RoundedCornerShape(24.dp)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(24.dp)),
        scale = CardDefaults.scale(focusedScale = 1.04f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected || isFocused) {
                    NuvioTheme.colors.TextPrimary
                } else {
                    NuvioTheme.colors.TextSecondary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Focusable circular color swatch for the accent presets. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflySwatch(
    color: Color,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = color,
            focusedContainerColor = color
        ),
        border = CardDefaults.border(
            border = if (selected) {
                Border(
                    border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                    shape = CircleShape
                )
            } else {
                Border.None
            },
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(2.dp),
                shape = CircleShape
            )
        ),
        shape = CardDefaults.shape(CircleShape),
        scale = CardDefaults.scale(focusedScale = 1.12f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (selected || isFocused) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/** Vertical spacer used between dialog sections. */
@Composable
fun NetflySectionSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

/** Horizontal arrangement helper: evenly spaced row of items. */
fun netflyRowArrangement(): Arrangement.Horizontal =
    Arrangement.spacedBy(12.dp)
