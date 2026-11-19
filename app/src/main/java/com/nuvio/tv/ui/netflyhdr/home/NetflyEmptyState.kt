package com.nuvio.tv.ui.netflyhdr.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.addon.AddonManagerViewModel
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Home empty state (design spec §4): title + explainer + a 400×400 QR code
 * for setting up addons from the phone.
 *
 * The QR is the REAL addon web-setup URL: it reuses
 * [AddonManagerViewModel.startQrMode], which starts the local addon config
 * web server and generates the QR from its URL — the same QR the Addon
 * Manager shows. The server stops when this state leaves the screen. When no
 * network/server is available, the QR is replaced by an honest explainer
 * (no fake code is ever shown).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyEmptyState(
    title: String,
    explainer: String,
    addonViewModel: AddonManagerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val addonState by addonViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        addonViewModel.startQrMode()
    }
    DisposableEffect(Unit) {
        onDispose { addonViewModel.stopQrMode() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 96.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = NuvioTheme.colors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = explainer,
                style = MaterialTheme.typography.bodyLarge,
                color = NuvioTheme.colors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            val qrBitmap = addonState.qrCodeBitmap
            if (qrBitmap != null) {
                // 400×400 px on the 1080p canvas (200 dp at xhdpi density).
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.netfly_empty_qr_description),
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.netfly_empty_qr_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextTertiary,
                    textAlign = TextAlign.Center
                )
                val serverUrl = addonState.serverUrl
                if (serverUrl != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = serverUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioTheme.colors.TextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.netfly_empty_qr_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
