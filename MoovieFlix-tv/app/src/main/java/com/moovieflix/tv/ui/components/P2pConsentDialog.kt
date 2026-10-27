package com.moovieflix.tv.ui.components

import com.moovieflix.tv.ui.theme.MoovieFlixTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.moovieflix.tv.R
import com.moovieflix.tv.ui.util.contentLayoutDirection
import com.moovieflix.tv.ui.util.contentTextDirection
import kotlinx.coroutines.launch
import com.moovieflix.tv.ui.screens.ambient.AmbientDialogEffect

@Composable
fun P2pConsentDialog(
    onEnableP2p: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val bodyText = stringResource(R.string.p2p_consent_body)
    val layoutDirection = remember(bodyText) { bodyText.contentLayoutDirection() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AmbientDialogEffect(onDismiss)
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(MoovieFlixTheme.radii.xl))
                    .background(MoovieFlixTheme.colors.BackgroundCard)
            ) {
                Column(
                    modifier = Modifier
                        .width(520.dp) // Increased width from 460.dp to 520.dp for better text distribution
                        .padding(MoovieFlixTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.p2p_consent_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MoovieFlixTheme.colors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(MoovieFlixTheme.spacing.lg))

                    val scrollState = rememberScrollState()
                    val coroutineScope = rememberCoroutineScope()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionDown -> {
                                            if (scrollState.value < scrollState.maxValue) {
                                                coroutineScope.launch { scrollState.animateScrollBy(100f) }
                                                true
                                            } else false
                                        }
                                        Key.DirectionUp -> {
                                            if (scrollState.value > 0) {
                                                coroutineScope.launch { scrollState.animateScrollBy(-100f) }
                                                true
                                            } else false
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .focusable()
                            // Merge the body copy into this focusable scroll node so it is
                            // announced once, instead of as an unnamed container.
                            .semantics(mergeDescendants = true) {
                                contentDescription = bodyText
                            }
                    ) {
                        Text(
                            text = bodyText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = bodyText.contentTextDirection()
                            ),
                            color = MoovieFlixTheme.colors.TextSecondary,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(end = MoovieFlixTheme.spacing.xs)
                        )
                    }

                    Spacer(modifier = Modifier.height(MoovieFlixTheme.spacing.xl))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.lg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            colors = CardDefaults.colors(
                                containerColor = MoovieFlixTheme.colors.BackgroundElevated,
                                focusedContainerColor = MoovieFlixTheme.colors.BackgroundElevated
                            ),
                            border = CardDefaults.border(
                                focusedBorder = Border(
                                    border = MoovieFlixTheme.focusRing.border(MoovieFlixTheme.spacing.xxs),
                                    shape = RoundedCornerShape(MoovieFlixTheme.radii.md)
                                )
                            ),
                            shape = CardDefaults.shape(shape = RoundedCornerShape(MoovieFlixTheme.radii.md)),
                            scale = CardDefaults.scale(focusedScale = 1.05f)
                        ) {
                            Text(
                                text = stringResource(R.string.p2p_consent_cancel),
                                style = MaterialTheme.typography.titleMedium,
                                color = MoovieFlixTheme.colors.TextPrimary,
                                modifier = Modifier
                                    .padding(horizontal = MoovieFlixTheme.spacing.lg, vertical = 14.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        var enableFocused by remember { mutableStateOf(false) }
                        Card(
                            onClick = onEnableP2p,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { enableFocused = it.isFocused },
                            colors = CardDefaults.colors(
                                containerColor = MoovieFlixTheme.colors.BackgroundElevated,
                                focusedContainerColor = MoovieFlixTheme.colors.Secondary
                            ),
                            border = CardDefaults.border(
                                focusedBorder = Border(
                                    border = MoovieFlixTheme.focusRing.border(MoovieFlixTheme.spacing.xxs),
                                    shape = RoundedCornerShape(MoovieFlixTheme.radii.md)
                                )
                            ),
                            shape = CardDefaults.shape(shape = RoundedCornerShape(MoovieFlixTheme.radii.md)),
                            scale = CardDefaults.scale(focusedScale = 1.05f)
                        ) {
                            Text(
                                text = stringResource(R.string.p2p_consent_enable),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (enableFocused) MoovieFlixTheme.colors.OnSecondary else MoovieFlixTheme.colors.TextPrimary,
                                modifier = Modifier
                                    .padding(horizontal = MoovieFlixTheme.spacing.lg, vertical = 14.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
