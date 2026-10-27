package com.moovieflix.app.features.profiles

import com.moovieflix.app.core.ui.MoovieFlixToastController
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.profile_already_active
import org.jetbrains.compose.resources.getString

internal fun routeProfileSelection(
    profile: MoovieFlixProfile,
    isEditMode: Boolean,
    activeProfileIndex: Int? = null,
    onEditProfile: (MoovieFlixProfile) -> Unit,
    onActiveProfileSelected: (MoovieFlixProfile) -> Unit,
    onPinRequired: (MoovieFlixProfile) -> Unit,
    onProfileSelected: (MoovieFlixProfile) -> Unit,
) {
    when {
        isEditMode -> onEditProfile(profile)
        profile.profileIndex == activeProfileIndex -> onActiveProfileSelected(profile)
        profile.pinEnabled -> onPinRequired(profile)
        else -> onProfileSelected(profile)
    }
}

internal suspend fun showAlreadyActiveProfileToast(profile: MoovieFlixProfile) {
    MoovieFlixToastController.show(getString(Res.string.profile_already_active, profile.name))
}
