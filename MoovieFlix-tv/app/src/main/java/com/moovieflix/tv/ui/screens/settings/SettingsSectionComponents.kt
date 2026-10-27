@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.moovieflix.tv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.moovieflix.tv.R
import com.moovieflix.tv.ui.theme.MoovieFlixTheme

internal val SettingsRowContentInset = 18.dp

internal enum class SettingsNoteTone { Info, Warning, Danger }

@Composable
internal fun SettingsCollapsibleSection(
    title: String,
    description: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.sm)
    ) {
        SettingsActionRow(
            title = title,
            subtitle = description,
            value = stringResource(if (expanded) R.string.layout_open else R.string.layout_closed),
            onClick = onToggle,
            leadingIcon = icon,
            trailingIcon = if (expanded) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            modifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            onFocused = onFocused
        )

        if (expanded) {
            SettingsGroupCard(modifier = Modifier.padding(start = MoovieFlixTheme.spacing.lg)) {
                content()
            }
        }
    }
}

@Composable
internal fun SettingsSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SettingsRowContentInset,
                end = SettingsRowContentInset,
                top = MoovieFlixTheme.spacing.md,
                bottom = MoovieFlixTheme.spacing.xxs
            ),
        verticalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.xxs)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.2.sp,
            color = MoovieFlixTheme.colors.TextTertiary
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MoovieFlixTheme.colors.TextTertiary
            )
        }
    }
}

@Composable
internal fun SettingsNote(
    text: String,
    modifier: Modifier = Modifier,
    tone: SettingsNoteTone = SettingsNoteTone.Info
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = when (tone) {
            SettingsNoteTone.Info -> MoovieFlixTheme.colors.TextSecondary
            SettingsNoteTone.Warning -> MoovieFlixTheme.colors.Warning
            SettingsNoteTone.Danger -> MoovieFlixTheme.colors.Error
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SettingsRowContentInset,
                end = SettingsRowContentInset,
                top = MoovieFlixTheme.spacing.xxs,
                bottom = MoovieFlixTheme.spacing.xs
            )
    )
}

@Composable
internal fun SettingsRailDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SettingsRowContentInset, end = SettingsRowContentInset, bottom = 10.dp)
            .height(MoovieFlixTheme.spacing.hairline)
            .background(MoovieFlixTheme.colors.Border)
    )
}

@Composable
internal fun SettingsTopBarDivider() {
    Box(
        modifier = Modifier
            .padding(end = MoovieFlixTheme.spacing.sm)
            .width(MoovieFlixTheme.spacing.hairline)
            .height(20.dp)
            .background(MoovieFlixTheme.colors.Border)
    )
}

@Composable
internal fun SettingsResetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {}
) {
    val flat = isFlatSettingsStyle()
    val shape = if (flat) settingsRowShape() else RoundedCornerShape(SettingsPillRadius)
    Button(
        onClick = onClick,
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocused() },
        shape = ButtonDefaults.shape(shape = shape),
        colors = ButtonDefaults.colors(
            containerColor = MoovieFlixTheme.colors.Background,
            focusedContainerColor = if (flat) settingsFocusFillColor() else MoovieFlixTheme.colors.Background
        ),
        border = if (flat) {
            ButtonDefaults.border(border = Border.None, focusedBorder = Border.None)
        } else {
            ButtonDefaults.border(
                focusedBorder = Border(
                    border = MoovieFlixTheme.focusRing.border(MoovieFlixTheme.spacing.xxs),
                    shape = shape
                )
            )
        },
        scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                tint = MoovieFlixTheme.colors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(MoovieFlixTheme.spacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MoovieFlixTheme.colors.TextPrimary
            )
        }
    }
}
