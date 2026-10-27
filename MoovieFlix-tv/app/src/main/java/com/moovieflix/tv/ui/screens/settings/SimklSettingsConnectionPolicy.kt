package com.moovieflix.tv.ui.screens.settings

import com.moovieflix.tv.data.simkl.SimklAuthState
import com.moovieflix.tv.data.simkl.SimklConnectionMode

internal fun shouldRefreshMissingSimklIdentity(
    state: SimklAuthState,
    isRefreshBlocked: Boolean
): Boolean = state.isAuthenticated && state.username.isNullOrBlank() && !isRefreshBlocked

internal fun shouldCancelSimklPolling(mode: SimklConnectionMode): Boolean =
    mode == SimklConnectionMode.DISCONNECTED
