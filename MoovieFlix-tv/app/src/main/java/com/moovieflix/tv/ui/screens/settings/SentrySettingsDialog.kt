package com.moovieflix.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.moovieflix.tv.R
import com.moovieflix.tv.ui.components.MoovieFlixDialog
import com.moovieflix.tv.ui.theme.MoovieFlixTheme

@Composable
internal fun SentrySettingsDialog(
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }

    LaunchedEffect(enabled) {
        if (enabled) {
            cancelFocusRequester.requestFocus()
        } else {
            confirmFocusRequester.requestFocus()
        }
    }

    MoovieFlixDialog(
        onDismiss = onDismiss,
        title = stringResource(
            if (enabled) {
                R.string.sentry_disable_dialog_title
            } else {
                R.string.sentry_enable_dialog_title
            }
        ),
        subtitle = stringResource(
            if (enabled) {
                R.string.sentry_disable_dialog_subtitle
            } else {
                R.string.sentry_enable_dialog_subtitle
            }
        ),
        width = 640.dp,
        suppressFirstKeyUp = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.md)
        ) {
            SentryInfoSection(
                title = stringResource(R.string.sentry_help_title),
                body = stringResource(R.string.sentry_help_body)
            )
            SentryInfoSection(
                title = stringResource(R.string.sentry_sent_title),
                body = stringResource(R.string.sentry_sent_body)
            )
            SentryInfoSection(
                title = stringResource(R.string.sentry_not_sent_title),
                body = stringResource(R.string.sentry_not_sent_body)
            )
        }

        Spacer(modifier = Modifier.height(MoovieFlixTheme.spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.md)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(cancelFocusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = MoovieFlixTheme.colors.BackgroundCard,
                    contentColor = MoovieFlixTheme.colors.TextPrimary
                )
            ) {
                Text(
                    text = stringResource(
                        if (enabled) {
                            R.string.sentry_keep_enabled
                        } else {
                            R.string.action_cancel
                        }
                    )
                )
            }
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(confirmFocusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = MoovieFlixTheme.colors.BackgroundCard,
                    contentColor = MoovieFlixTheme.colors.TextPrimary
                )
            ) {
                Text(
                    text = stringResource(
                        if (enabled) {
                            R.string.sentry_turn_off
                        } else {
                            R.string.sentry_turn_on
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun SentryInfoSection(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.xxs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MoovieFlixTheme.colors.TextPrimary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MoovieFlixTheme.colors.TextSecondary
        )
    }
}
