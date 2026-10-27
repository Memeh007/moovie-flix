package com.moovieflix.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.moovieflix.tv.R
import com.moovieflix.tv.domain.model.MemberTier
import com.moovieflix.tv.ui.theme.MoovieFlixTheme

@Composable
internal fun DebugMemberTierCard(
    selectedTier: MemberTier?,
    onTierSelected: (MemberTier?) -> Unit
) {
    val options = listOf(
        null to stringResource(R.string.debug_member_tier_none),
        MemberTier.SUPPORTER to stringResource(R.string.debug_member_tier_one),
        MemberTier.SUPPORTER_PLUS to stringResource(R.string.debug_member_tier_two)
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.debug_member_tier_title),
                style = MaterialTheme.typography.titleMedium,
                color = MoovieFlixTheme.colors.TextPrimary
            )
            Text(
                text = stringResource(R.string.debug_member_tier_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MoovieFlixTheme.colors.TextSecondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.sm)
        ) {
            options.forEach { (tier, label) ->
                val selected = tier == selectedTier
                val shape = RoundedCornerShape(MoovieFlixTheme.radii.sm)
                Card(
                    onClick = { onTierSelected(tier) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.RadioButton
                            this.selected = selected
                        },
                    colors = CardDefaults.colors(
                        containerColor = if (selected) {
                            MoovieFlixTheme.colors.Secondary.copy(alpha = 0.2f)
                        } else {
                            MoovieFlixTheme.colors.BackgroundCard
                        },
                        focusedContainerColor = MoovieFlixTheme.colors.FocusBackground
                    ),
                    border = CardDefaults.border(
                        border = if (selected) {
                            Border(
                                border = MoovieFlixTheme.focusRing.border(MoovieFlixTheme.spacing.hairline),
                                shape = shape
                            )
                        } else {
                            Border.None
                        },
                        focusedBorder = Border(
                            border = MoovieFlixTheme.focusRing.border(MoovieFlixTheme.spacing.xxs),
                            shape = shape
                        )
                    ),
                    shape = CardDefaults.shape(shape),
                    scale = CardDefaults.scale(focusedScale = 1.04f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MoovieFlixTheme.colors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MoovieFlixTheme.spacing.md, vertical = MoovieFlixTheme.spacing.lg),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
