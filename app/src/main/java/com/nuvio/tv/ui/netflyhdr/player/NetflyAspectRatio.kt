package com.nuvio.tv.ui.netflyhdr.player

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.netflyhdr.NetflyRed
import com.nuvio.tv.ui.screens.player.AspectMode

/**
 * Netfly HDR player controls (design §13) — inventory and mapping.
 *
 * The base player already has every §13 control as a round
 * [androidx.tv.material3.IconButton] ([androidx.compose.foundation.shape.CircleShape],
 * 48dp with 24-28dp icons — the Roku spec's 72px/36px physical pixels map to
 * roughly 36-54dp at TV densities, so the base size sits in the spec band
 * and is kept as-is):
 *
 * - Play/pause, subtitles, audio, sources: main control row.
 * - Next episode + episodes (series only): main control row, shown when an
 *   episode context exists.
 * - Aspect ratio: main control row (moved out of the "more" row by this
 *   branch); opens [NetflyAspectRatioPicker].
 * - Open-method toggle: player-engine switch (ExoPlayer ↔ MPV) and
 *   open-in-external-player, in the "more" row.
 * - Streams screen: the base `StreamScreen` + sources side panel.
 *
 * Watch Party is intentionally NOT built here: it is experimental even in
 * Netfly itself, and the base's party button is left untouched.
 */
object NetflyPlayerControls

/**
 * The three Netfly aspect-ratio options (design §13, the 4:3 feature),
 * mapped onto the base [AspectMode]s, which the player already applies to
 * the Media3 (ExoPlayer) surface and the MPV surface and persists per
 * device (`DeviceLocalPlayerPreferences`).
 */
enum class NetflyAspectRatioOption(
    val aspectMode: AspectMode,
    @StringRes val labelResId: Int,
    @StringRes val descriptionResId: Int
) {
    ORIGINAL(
        AspectMode.ORIGINAL,
        R.string.netfly_aspect_original,
        R.string.netfly_aspect_original_desc
    ),
    ZOOM(
        AspectMode.FULL_SCREEN,
        R.string.netfly_aspect_zoom,
        R.string.netfly_aspect_zoom_desc
    ),
    STRETCH(
        AspectMode.STRETCH,
        R.string.netfly_aspect_stretch,
        R.string.netfly_aspect_stretch_desc
    );

    companion object {
        /**
         * Best Netfly option for a base mode. Zoom-family modes
         * (slight/cinema zoom) read as [ZOOM]; partial-stretch modes read as
         * [STRETCH]; anything else falls back to [ORIGINAL].
         */
        fun fromAspectMode(mode: AspectMode): NetflyAspectRatioOption =
            when (mode) {
                AspectMode.ORIGINAL -> ORIGINAL
                AspectMode.FULL_SCREEN,
                AspectMode.SLIGHT_ZOOM,
                AspectMode.CINEMA_ZOOM -> ZOOM

                AspectMode.STRETCH,
                AspectMode.VERTICAL_STRETCH,
                AspectMode.HORIZONTAL_STRETCH -> STRETCH
            }
    }
}

/**
 * Aspect-ratio picker dialog: the three Netfly options with the current one
 * highlighted in Netfly red. The caller dispatches the choice with
 * `PlayerEvent.OnSetAspectRatio(option.aspectMode)`.
 */
@Composable
fun NetflyAspectRatioPicker(
    currentMode: AspectMode,
    onOptionSelected: (NetflyAspectRatioOption) -> Unit,
    onDismiss: () -> Unit
) {
    val currentOption = NetflyAspectRatioOption.fromAspectMode(currentMode)
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.netfly_aspect_title)
    ) {
        NetflyAspectRatioOption.entries.forEach { option ->
            val isSelected = option == currentOption
            Button(
                onClick = {
                    onOptionSelected(option)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = if (isSelected) {
                    ButtonDefaults.colors(
                        containerColor = NetflyRed,
                        focusedContainerColor = NetflyRed
                    )
                } else {
                    ButtonDefaults.colors()
                }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(option.labelResId),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(option.descriptionResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            Color.White.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
