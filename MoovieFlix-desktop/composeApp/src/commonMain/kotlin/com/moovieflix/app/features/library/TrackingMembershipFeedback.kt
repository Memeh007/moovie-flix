package com.moovieflix.app.features.library

import com.moovieflix.app.core.ui.MoovieFlixToastController
import com.moovieflix.app.features.tracking.TrackingLibraryTab
import com.moovieflix.app.features.tracking.TrackingMembershipApplyResult
import com.moovieflix.app.features.tracking.TrackingProviderRegistry
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tracking_list_status_rewritten
import org.jetbrains.compose.resources.getString

internal suspend fun showTrackingMembershipRewriteFeedback(result: TrackingMembershipApplyResult) {
    val rewrite = result.rewrites.firstOrNull() ?: return
    val providerName = TrackingProviderRegistry.authProvider(rewrite.providerId)
        ?.descriptor
        ?.displayName
        ?: rewrite.providerId.storageId.replaceFirstChar { char -> char.titlecase() }
    val tabs = TrackingProviderRegistry.libraryProvider(rewrite.providerId)?.snapshot()?.tabs.orEmpty()
    val requestedTitle = tabs.statusTitle(rewrite.requestedListKey, providerName)
    val resolvedTitle = tabs.statusTitle(rewrite.resolvedListKey, providerName)
    MoovieFlixToastController.show(
        getString(
            Res.string.tracking_list_status_rewritten,
            providerName,
            resolvedTitle,
            requestedTitle,
        ),
    )
}

private fun List<TrackingLibraryTab>.statusTitle(
    key: String,
    providerName: String,
): String = firstOrNull { tab -> tab.key == key }
    ?.title
    ?.removePrefix("$providerName ")
    ?: key.substringAfterLast(':')
