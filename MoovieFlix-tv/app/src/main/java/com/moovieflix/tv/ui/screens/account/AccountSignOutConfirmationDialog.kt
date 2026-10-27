@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.moovieflix.tv.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.moovieflix.tv.R
import com.moovieflix.tv.ui.components.MoovieFlixDialog
import com.moovieflix.tv.ui.theme.MoovieFlixPrimitives
import com.moovieflix.tv.ui.theme.MoovieFlixTheme

@Composable
internal fun AccountSignOutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    MoovieFlixDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.account_sign_out_confirm_title),
        subtitle = stringResource(R.string.account_sign_out_confirm_subtitle),
        suppressFirstKeyUp = false
    ) {
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
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.colors(
                    containerColor = MoovieFlixPrimitives.red600.copy(alpha = 0.25f),
                    contentColor = Color(0xFFF44336)
                )
            ) {
                Text(stringResource(R.string.account_sign_out))
            }
        }
    }
}
