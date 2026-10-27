package com.moovieflix.tv.ui.screens.collection

import com.moovieflix.tv.ui.theme.MoovieFlixTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.moovieflix.tv.domain.model.AddonCatalogCollectionSource
import com.moovieflix.tv.domain.model.catalogTypesMatch
import com.moovieflix.tv.domain.model.CollectionFolder
import com.moovieflix.tv.domain.model.CollectionSource
import com.moovieflix.tv.domain.model.FolderViewMode
import com.moovieflix.tv.domain.model.PosterShape
import com.moovieflix.tv.domain.model.TmdbCollectionFilters
import com.moovieflix.tv.domain.model.TmdbCollectionMediaType
import com.moovieflix.tv.domain.model.TmdbCollectionSort
import com.moovieflix.tv.domain.model.TmdbCollectionSource
import com.moovieflix.tv.domain.model.TmdbCollectionSourceType
import com.moovieflix.tv.ui.components.LoadingIndicator
import com.moovieflix.tv.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CatalogPickerContent(
    catalogs: List<AvailableCatalog>,
    alreadyAdded: List<CollectionSource>,
    onToggle: (AvailableCatalog) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = MoovieFlixTheme.spacing.xxxl, start = MoovieFlixTheme.spacing.xxxl, end = MoovieFlixTheme.spacing.xxxl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.collections_editor_select_catalogs),
                style = MaterialTheme.typography.headlineMedium,
                color = MoovieFlixTheme.colors.TextPrimary
            )
            MoovieFlixButton(onClick = onBack) { Text(stringResource(R.string.collections_editor_done)) }
        }

        Spacer(modifier = Modifier.height(MoovieFlixTheme.spacing.lg))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = MoovieFlixTheme.spacing.sm, end = MoovieFlixTheme.spacing.sm, top = MoovieFlixTheme.spacing.xs, bottom = MoovieFlixTheme.spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(MoovieFlixTheme.spacing.sm)
        ) {
            itemsIndexed(
                items = catalogs,
                key = { index, c -> "${c.addonId}_${c.type}_${c.catalogId}_$index" }
            ) { _, catalog ->
                val isAdded = alreadyAdded.any {
                    it is AddonCatalogCollectionSource && it.addonId == catalog.addonId && catalogTypesMatch(it.type, catalog.type) && it.catalogId == catalog.catalogId
                }
                Card(
                    onClick = { onToggle(catalog) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            role = Role.Checkbox
                            toggleableState = if (isAdded) ToggleableState.On else ToggleableState.Off
                        },
                    colors = CardDefaults.colors(
                        containerColor = if (isAdded) MoovieFlixTheme.colors.Secondary.copy(alpha = 0.15f) else MoovieFlixTheme.colors.BackgroundCard,
                        focusedContainerColor = MoovieFlixTheme.colors.FocusBackground
                    ),
                    border = CardDefaults.border(
                        border = if (isAdded) Border(
                            border = BorderStroke(MoovieFlixTheme.spacing.hairline, MoovieFlixTheme.colors.Secondary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(MoovieFlixTheme.radii.md)
                        ) else Border.None,
                        focusedBorder = Border(
                            border = MoovieFlixTheme.focusRing.border(MoovieFlixTheme.spacing.xxs),
                            shape = RoundedCornerShape(MoovieFlixTheme.radii.md)
                        )
                    ),
                    shape = CardDefaults.shape(RoundedCornerShape(MoovieFlixTheme.radii.md)),
                    scale = CardDefaults.scale(focusedScale = 1.01f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MoovieFlixTheme.spacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = catalog.catalogName.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall,
                                color = MoovieFlixTheme.colors.TextPrimary
                            )
                            val supportingGenreText = when {
                                catalog.genreRequired -> stringResource(R.string.collections_editor_genre_required)
                                catalog.genreOptions.isNotEmpty() -> stringResource(R.string.collections_editor_genre_optional)
                                else -> null
                            }
                            Text(
                                text = listOfNotNull("${catalog.type} - ${catalog.addonName}", supportingGenreText).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MoovieFlixTheme.colors.TextTertiary
                            )
                        }
                        if (isAdded) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MoovieFlixTheme.colors.TextSecondary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MoovieFlixTheme.colors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
