package com.nuvio.tv.ui.netflyhdr.themes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Theme Manager (design spec §3): a 1760×980 dialog over a 92% dim showing
 * the 10 red Netfly presets as mini preview cards, the dark/light toggle,
 * the accent override and the reduce-motion switch.
 *
 * Behaviour: arrows browse, focusing a card live-previews it across the app,
 * OK applies (confirms) and closes, BACK cancels the preview and restores
 * the theme that was active when the manager opened.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyThemeManagerDialog(
    viewModel: NetflyThemeViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val entryState by viewModel.entryState.collectAsStateWithLifecycle()
    var confirmed by remember { mutableStateOf(false) }
    var focusedPreset by remember { mutableStateOf<NetflyThemePreset?>(null) }
    var showHexEditor by remember { mutableStateOf(false) }

    fun close() {
        if (!confirmed) {
            val entry = entryState
            if (entry != null) {
                viewModel.restoreEntryTheme(entry.netflyThemeEnabled, entry.preset)
            }
        }
        onDismiss()
    }

    val appliedFocusRequester = remember { FocusRequester() }
    LaunchedEffect(entryState) {
        if (entryState != null) {
            appliedFocusRequester.requestFocusAfterFrames()
        }
    }

    NetflyDialogShell(onDismiss = ::close) {
        NetflyDialogHeader(
            title = stringResource(R.string.netfly_theme_manager_title),
            redLabel = stringResource(R.string.netfly_theme_presets_label),
            hint = stringResource(R.string.netfly_theme_hint)
        )
        Spacer(modifier = Modifier.height(16.dp))

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dark / light toggle pills (176×48 per spec).
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NetflyOptionPill(
                    text = stringResource(R.string.netfly_theme_dark),
                    selected = !state.lightMode,
                    onClick = { viewModel.setLightMode(false) },
                    modifier = Modifier.width(176.dp).height(48.dp)
                )
                NetflyOptionPill(
                    text = stringResource(R.string.netfly_theme_light),
                    selected = state.lightMode,
                    onClick = { viewModel.setLightMode(true) },
                    modifier = Modifier.width(176.dp).height(48.dp)
                )
            }

            // The 10 preset preview cards (5 × 2 grid).
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(NetflyThemePreset.entries, key = { it.name }) { preset ->
                    val isApplied = state.netflyThemeEnabled && preset == state.preset
                    val isFocused = preset == focusedPreset
                    NetflyThemePreviewCard(
                        preset = preset,
                        isApplied = isApplied,
                        isPreview = isFocused && !isApplied,
                        modifier = if (preset == state.preset) {
                            Modifier.focusRequester(appliedFocusRequester)
                        } else {
                            Modifier
                        },
                        onFocus = {
                            focusedPreset = preset
                            if (entryState != null) {
                                viewModel.previewPreset(preset)
                            }
                        },
                        onClick = {
                            confirmed = true
                            viewModel.confirmPreset(preset)
                            onDismiss()
                        }
                    )
                }
            }

            // Accent override: 7 presets + custom hex + reset.
            NetflySectionLabel(text = stringResource(R.string.netfly_theme_accent_title))
            Text(
                text = stringResource(R.string.netfly_theme_accent_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextTertiary
            )
            val currentAccent = state.accentOverride ?: NETFLY_RED
            val selectedAccentPreset = NetflyAccentPreset.fromColor(currentAccent)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NetflyAccentPreset.entries.forEach { accent ->
                    NetflySwatch(
                        color = accent.color,
                        selected = accent == selectedAccentPreset,
                        contentDescription = stringResource(accent.nameRes),
                        onClick = {
                            viewModel.setAccent(
                                if (accent == NetflyAccentPreset.NETFLY_RED) null else accent.color
                            )
                        }
                    )
                }
                NetflyOptionPill(
                    text = stringResource(R.string.netfly_theme_accent_custom),
                    selected = selectedAccentPreset == null,
                    onClick = { showHexEditor = true }
                )
                NetflyOptionPill(
                    text = stringResource(R.string.netfly_theme_accent_reset),
                    selected = false,
                    onClick = { viewModel.setAccent(null) }
                )
            }

            NetflyToggleRow(
                title = stringResource(R.string.netfly_theme_reduce_motion),
                subtitle = stringResource(R.string.netfly_theme_reduce_motion_subtitle),
                checked = state.reduceMotion,
                onToggle = { viewModel.setReduceMotion(!state.reduceMotion) }
            )

            NetflyOptionPill(
                text = stringResource(R.string.netfly_theme_back_to_nuvio),
                selected = false,
                onClick = {
                    confirmed = true
                    viewModel.backToNuvioThemes()
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showHexEditor) {
        NetflyHexEditorDialog(
            initialColor = currentAccent(state),
            onApply = { hex ->
                viewModel.setCustomHex(hex)
                showHexEditor = false
            },
            onDismiss = { showHexEditor = false }
        )
    }
}

private fun currentAccent(state: NetflyHdrState): Color =
    state.accentOverride ?: NETFLY_RED

/**
 * One mini preview card: the preset's background with a top bar, a nav strip,
 * a panel, 3 tiles and the red accent line, plus the name and the
 * APPLIED / PREVIEW badge.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyThemePreviewCard(
    preset: NetflyThemePreset,
    isApplied: Boolean,
    isPreview: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier.onFocusChanged {
            if (it.isFocused) {
                isFocused = true
                onFocus()
            } else {
                isFocused = false
            }
        },
        colors = CardDefaults.colors(
            containerColor = preset.background,
            focusedContainerColor = preset.background
        ),
        border = CardDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isApplied) NETFLY_RED else NuvioTheme.colors.Border
                ),
                shape = RoundedCornerShape(12.dp)
            ),
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(2.dp),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        scale = CardDefaults.scale(focusedScale = 1.04f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Mini preview: top bar, nav strip, panel, 3 tiles, red line.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(preset.background)
            ) {
                // Top bar.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(preset.surface2)
                )
                // Left nav strip.
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(width = 22.dp, height = 74.dp)
                        .background(preset.surface)
                )
                // Panel.
                Box(
                    modifier = Modifier
                        .padding(start = 28.dp, top = 16.dp)
                        .size(width = 52.dp, height = 30.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(preset.surface2)
                )
                // 3 tiles.
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 28.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(width = 22.dp, height = 14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(preset.surface2)
                        )
                    }
                }
                // Red accent line.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(NETFLY_RED)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(preset.nameRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused || isApplied) Color.White else Color(0xFFF2F2F7),
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isApplied) {
                    NetflyBadge(
                        text = stringResource(R.string.netfly_theme_applied),
                        filled = true
                    )
                } else if (isPreview) {
                    NetflyBadge(
                        text = stringResource(R.string.netfly_theme_preview),
                        filled = false
                    )
                }
            }
            Text(
                text = stringResource(preset.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyBadge(text: String, filled: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (filled) NETFLY_RED else Color.Transparent)
            .border(
                width = 1.dp,
                color = NETFLY_RED,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

/**
 * TV-friendly custom hex editor: 6 digit cells, Left/Right moves between
 * them, Up/Down cycles the hex digit, OK applies. No on-screen keyboard.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyHexEditorDialog(
    initialColor: Color,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHex = remember(initialColor) {
        formatNetflyHexColor(initialColor).removePrefix("#").toCharArray()
    }
    var digits by remember { mutableStateOf(initialHex.copyOf()) }
    var activeIndex by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) {
        focusRequester.requestFocusAfterFrames()
    }

    val currentHex = remember(digits) { "#" + digits.concatToString() }
    val currentColor = remember(digits) {
        parseNetflyHexColor(currentHex) ?: NETFLY_RED
    }

    NetflyDialogShell(onDismiss = onDismiss) {
        NetflyDialogHeader(
            title = stringResource(R.string.netfly_theme_hex_title),
            redLabel = currentHex,
            hint = stringResource(R.string.netfly_theme_hex_hint)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentColor)
                    .border(1.dp, NuvioTheme.colors.Border, RoundedCornerShape(12.dp))
            )
            Row(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionLeft -> {
                                activeIndex = (activeIndex + digits.size - 1) % digits.size
                                true
                            }

                            Key.DirectionRight -> {
                                activeIndex = (activeIndex + 1) % digits.size
                                true
                            }

                            Key.DirectionUp -> {
                                val next = digits.copyOf()
                                val pos = HEX_DIGITS.indexOf(next[activeIndex])
                                next[activeIndex] = HEX_DIGITS[(pos + 1) % HEX_DIGITS.size]
                                digits = next
                                true
                            }

                            Key.DirectionDown -> {
                                val next = digits.copyOf()
                                val pos = HEX_DIGITS.indexOf(next[activeIndex])
                                next[activeIndex] =
                                    HEX_DIGITS[(pos + HEX_DIGITS.size - 1) % HEX_DIGITS.size]
                                digits = next
                                true
                            }

                            Key.Enter, Key.NumPadEnter -> {
                                onApply(currentHex)
                                true
                            }

                            else -> false
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.displaySmall,
                    color = NuvioTheme.colors.TextPrimary
                )
                digits.forEachIndexed { index, digit ->
                    val active = index == activeIndex
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (active) {
                                    NuvioTheme.colors.FocusBackground
                                } else {
                                    NuvioTheme.colors.BackgroundCard
                                }
                            )
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = if (active) {
                                    NuvioTheme.colors.FocusRing
                                } else {
                                    NuvioTheme.colors.Border
                                },
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = NuvioTheme.colors.TextPrimary
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NetflyOptionPill(
                text = stringResource(R.string.action_apply),
                selected = false,
                onClick = { onApply(currentHex) }
            )
            NetflyOptionPill(
                text = stringResource(R.string.action_cancel),
                selected = false,
                onClick = onDismiss
            )
        }
    }
}
