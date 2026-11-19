package com.nuvio.tv.ui.netflyhdr.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.netflyhdr.NetflyRed

/**
 * Kids settings for the ACTIVE profile (design §11), hosted in the Profiles
 * settings category: a kids-mode toggle plus a maximum age-rating picker.
 * Settings are per-profile ([NetflyKidsProfileStore]); enforcement is via
 * [KidsContentFilter] in the home/search pipelines.
 */
@Composable
fun NetflyKidsProfileSection(
    viewModel: NetflyKidsProfileViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showAgeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.netfly_kids_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            onClick = { viewModel.setEnabled(!settings.enabled) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .semantics {
                    role = Role.Switch
                    toggleableState = if (settings.enabled) ToggleableState.On else ToggleableState.Off
                },
            colors = CardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.netfly_kids_mode_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.netfly_kids_mode_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                NetflyTogglePill(checked = settings.enabled)
            }
        }

        if (settings.enabled) {
            Card(
                onClick = { showAgeDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 62.dp),
                colors = CardDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.netfly_kids_max_age_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.netfly_kids_max_age_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(
                            R.string.netfly_kids_age_label,
                            settings.maxAgeYears
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NetflyRed
                    )
                }
            }
        }
    }

    if (showAgeDialog) {
        NuvioDialog(
            onDismiss = { showAgeDialog = false },
            title = stringResource(R.string.netfly_kids_max_age_dialog_title)
        ) {
            NetflyKidsSettings.MAX_AGE_OPTIONS.forEach { age ->
                val isSelected = age == settings.maxAgeYears
                Button(
                    onClick = {
                        viewModel.setMaxAgeYears(age)
                        showAgeDialog = false
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
                    Text(
                        text = stringResource(R.string.netfly_kids_age_option, age)
                    )
                }
            }
        }
    }
}

@Composable
private fun NetflyTogglePill(checked: Boolean) {
    val trackColor = if (checked) {
        NetflyRed.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackColor)
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
        )
    }
}
