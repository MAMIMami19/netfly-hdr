package com.nuvio.tv.ui.netflyhdr.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R

private val KEY_WIDTH = 36.dp
private val KEY_HEIGHT = 40.dp
private val KEY_GAP = 4.dp

/**
 * On-screen QWERTY keyboard for the Netfly search screen (§9: keyboard on
 * the left). D-pad navigable: every key is a TV button, so focus moves
 * between keys and onward to the results with the remote. The keyboard only
 * emits key events — the owning screen drives the query through
 * [com.nuvio.tv.ui.screens.search.SearchViewModel].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrKeyboard(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    firstKeyFocusRequester: FocusRequester? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KEY_GAP)
    ) {
        KeyboardRow("1234567890", onKey, firstKeyFocusRequester)
        KeyboardRow("QWERTYUIOP", onKey, null)
        KeyboardRow("ASDFGHJKL", onKey, null)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KEY_GAP, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (char in "ZXCVBNM") {
                KeyboardKey(label = char.toString(), onClick = { onKey(char.toString()) })
            }
            Button(
                onClick = onBackspace,
                modifier = Modifier
                    .width(KEY_WIDTH + 16.dp)
                    .height(KEY_HEIGHT),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = stringResource(R.string.netflyhdr_search_backspace),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KEY_GAP, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSpace,
                modifier = Modifier
                    .width(KEY_WIDTH * 3 + KEY_GAP * 2)
                    .height(KEY_HEIGHT),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = stringResource(R.string.netflyhdr_search_space),
                    fontSize = 13.sp
                )
            }
            Button(
                onClick = onClear,
                modifier = Modifier
                    .width(KEY_WIDTH * 2 + KEY_GAP)
                    .height(KEY_HEIGHT),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = stringResource(R.string.netflyhdr_search_clear),
                    fontSize = 13.sp
                )
            }
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .width(KEY_WIDTH * 2 + KEY_GAP)
                    .height(KEY_HEIGHT),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.netflyhdr_search_go),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun KeyboardRow(
    keys: String,
    onKey: (String) -> Unit,
    firstKeyFocusRequester: FocusRequester?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KEY_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ((index, char) in keys.withIndex()) {
            KeyboardKey(
                label = char.toString(),
                onClick = { onKey(char.toString()) },
                focusRequester = if (index == 0) firstKeyFocusRequester else null
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun KeyboardKey(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(KEY_WIDTH)
            .height(KEY_HEIGHT)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = label, fontSize = 15.sp)
    }
}
