package com.moovieflix.tv.ui.screens

import com.moovieflix.tv.ui.theme.MoovieFlixTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moovieflix.tv.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.moovieflix.tv.data.local.ExperienceModeDataStore
import com.moovieflix.tv.data.local.LayoutPreferenceDataStore
import com.moovieflix.tv.domain.model.ExperienceMode
import com.moovieflix.tv.domain.model.HomeLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ExperienceModeSelectionViewModel @Inject constructor(
    private val experienceModeDataStore: ExperienceModeDataStore,
    private val layoutPreferenceDataStore: LayoutPreferenceDataStore
) : ViewModel() {
    suspend fun choose(mode: ExperienceMode) {
        experienceModeDataStore.setMode(mode)
        if (mode == ExperienceMode.ESSENTIAL) {
            layoutPreferenceDataStore.setLayout(HomeLayout.MODERN)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ExperienceModeSelectionScreen(
    onContinue: (ExperienceMode) -> Unit,
    viewModel: ExperienceModeSelectionViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val essentialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        essentialFocusRequester.requestFocus()
    }

    fun choose(mode: ExperienceMode) {
        coroutineScope.launch {
            viewModel.choose(mode)
            onContinue(mode)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = MoovieFlixTheme.spacing.xxxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.experience_mode_choose_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MoovieFlixTheme.colors.TextPrimary
            )
            Spacer(modifier = Modifier.height(MoovieFlixTheme.spacing.md))
            Text(
                text = stringResource(R.string.experience_mode_choose_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MoovieFlixTheme.colors.TextSecondary
            )
            Spacer(modifier = Modifier.height(36.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ExperienceModeCard(
                    title = stringResource(R.string.experience_mode_essential),
                    subtitle = stringResource(R.string.experience_mode_essential_card_subtitle),
                    icon = Icons.Default.VideoSettings,
                    onClick = { choose(ExperienceMode.ESSENTIAL) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(essentialFocusRequester)
                )
                ExperienceModeCard(
                    title = stringResource(R.string.experience_mode_advanced),
                    subtitle = stringResource(R.string.experience_mode_advanced_card_subtitle),
                    icon = Icons.Default.Tune,
                    onClick = { choose(ExperienceMode.ADVANCED) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ExperienceModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(210.dp),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MoovieFlixTheme.colors.BackgroundCard,
            focusedContainerColor = MoovieFlixTheme.colors.FocusBackground
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(MoovieFlixTheme.spacing.hairline, MoovieFlixTheme.colors.Border),
                shape = RoundedCornerShape(MoovieFlixTheme.radii.md)
            ),
            focusedBorder = Border(
                border = MoovieFlixTheme.focusRing.border(MoovieFlixTheme.spacing.xxs),
                shape = RoundedCornerShape(MoovieFlixTheme.radii.md)
            )
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(MoovieFlixTheme.radii.md)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Card(
            onClick = onClick,
            colors = CardDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            shape = CardDefaults.shape(RoundedCornerShape(MoovieFlixTheme.radii.md)),
            scale = CardDefaults.scale(focusedScale = 1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MoovieFlixTheme.spacing.xl),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MoovieFlixTheme.colors.TextSecondary
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MoovieFlixTheme.colors.TextPrimary
                )
                Spacer(modifier = Modifier.height(MoovieFlixTheme.spacing.sm))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoovieFlixTheme.colors.TextSecondary
                )
            }
        }
    }
}
