package com.nuvio.tv.ui.netflyhdr.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.ui.netflyhdr.NetflyHdrBadgePink
import com.nuvio.tv.ui.netflyhdr.NetflyHdrRed

/**
 * Store card sizes (§10: 330×194 cards in a 5×3 grid at 1080p → dp at
 * TV density). Slightly taller than the literal 97dp so the name, metadata,
 * description and badge all stay legible.
 */
val STORE_CARD_WIDTH = 166.dp
val STORE_CARD_HEIGHT = 108.dp
val STORE_CARD_GAP = 12.dp

/** Badge shown at the bottom of a store card (§10). */
enum class StoreBadge {
    INSTALL,
    INSTALLED,
    UNAVAILABLE,
    SEARCH,
    ADD,
    MANAGE,
    REFRESH,
    INSTALLING,
    CHECKING
}

@Composable
private fun StoreBadge.label(): String = when (this) {
    StoreBadge.INSTALL -> stringResource(R.string.netflyhdr_store_badge_install)
    StoreBadge.INSTALLED -> stringResource(R.string.netflyhdr_store_badge_installed)
    StoreBadge.UNAVAILABLE -> stringResource(R.string.netflyhdr_store_badge_unavailable)
    StoreBadge.SEARCH -> stringResource(R.string.netflyhdr_store_badge_search)
    StoreBadge.ADD -> stringResource(R.string.netflyhdr_store_badge_add)
    StoreBadge.MANAGE -> stringResource(R.string.netflyhdr_store_badge_manage)
    StoreBadge.REFRESH -> stringResource(R.string.netflyhdr_store_badge_refresh)
    StoreBadge.INSTALLING -> stringResource(R.string.netflyhdr_store_installing)
    StoreBadge.CHECKING -> stringResource(R.string.netflyhdr_store_validating)
}

/**
 * One addon card in the store grid (§10): logo 86×82, name (2 lines),
 * metadata (author · language · scrapers · types), description (3 lines),
 * bottom badge. Red ring on focus. The card background is the theme
 * surface (spec #1B1C24 is a theme-owned surface, not a hardcoded color).
 *
 * Metadata is only shown when it is real: scraper counts and versions come
 * from the live manifest probe, never from the directory JSON.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrStoreCard(
    state: DirectoryEntryState,
    badge: StoreBadge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entry = state.entry
    val cardShape = remember { RoundedCornerShape(10.dp) }
    Card(
        onClick = onClick,
        modifier = modifier
            .width(STORE_CARD_WIDTH)
            .height(STORE_CARD_HEIGHT),
        shape = CardDefaults.shape(shape = cardShape),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, NetflyHdrRed),
                shape = cardShape
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StoreLogo(
                    logoUrl = state.liveLogo,
                    name = entry.name,
                    modifier = Modifier.size(43.dp, 41.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                    val scrapersLabel = state.liveScraperCount?.let {
                        stringResource(R.string.netflyhdr_store_scrapers, it)
                    }
                    val meta = remember(entry, state, scrapersLabel) {
                        buildList {
                            entry.author?.let { add(it) }
                            entry.language?.let { add(it) }
                            scrapersLabel?.let { add(it) }
                            if (entry.types.isNotEmpty()) {
                                add(entry.types.joinToString("/"))
                            }
                        }.joinToString(" · ")
                    }
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
            if (!entry.description.isNullOrBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Text(
                text = badge.label(),
                style = MaterialTheme.typography.labelMedium,
                color = NetflyHdrBadgePink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** Addon logo, or a monogram tile when no logo is known yet. */
@Composable
private fun StoreLogo(
    logoUrl: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    if (!logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                color = NetflyHdrRed
            )
        }
    }
}

/** Action cards come FIRST in the store grid (§10). */
enum class StoreAction {
    SEARCH,
    ADD_URL,
    REFRESH,
    INSTALLED
}

@Composable
private fun StoreAction.icon(): ImageVector = when (this) {
    StoreAction.SEARCH -> Icons.Default.Search
    StoreAction.ADD_URL -> Icons.Default.Add
    StoreAction.REFRESH -> Icons.Default.Refresh
    StoreAction.INSTALLED -> Icons.Default.Storage
}

@Composable
private fun StoreAction.title(): String = when (this) {
    StoreAction.SEARCH -> stringResource(R.string.netflyhdr_store_action_search)
    StoreAction.ADD_URL -> stringResource(R.string.netflyhdr_store_action_add_url)
    StoreAction.REFRESH -> stringResource(R.string.netflyhdr_store_action_refresh)
    StoreAction.INSTALLED -> stringResource(R.string.netflyhdr_store_action_installed)
}

@Composable
private fun StoreAction.badge(): StoreBadge = when (this) {
    StoreAction.SEARCH -> StoreBadge.SEARCH
    StoreAction.ADD_URL -> StoreBadge.ADD
    StoreAction.REFRESH -> StoreBadge.REFRESH
    StoreAction.INSTALLED -> StoreBadge.MANAGE
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrStoreActionCard(
    action: StoreAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(10.dp) }
    Card(
        onClick = onClick,
        modifier = modifier
            .width(STORE_CARD_WIDTH)
            .height(STORE_CARD_HEIGHT),
        shape = CardDefaults.shape(shape = cardShape),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, NetflyHdrRed),
                shape = cardShape
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = action.title(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.badge().label(),
                style = MaterialTheme.typography.labelMedium,
                color = NetflyHdrBadgePink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
