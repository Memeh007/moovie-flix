package com.moovieflix.app.features.membership

import com.moovieflix.app.features.profiles.MoovieFlixProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileBackgroundTest {
    @Test
    fun backgroundDoesNotRenderWithoutEntitlement() {
        val profile = MoovieFlixProfile(profileBackgroundId = "aurora")

        assertNull(resolveProfileBackground(profile, CosmeticEntitlements.None))
    }

    @Test
    fun customBackgroundTakesPriorityWhenEntitled() {
        val profile = MoovieFlixProfile(
            profileBackgroundId = "aurora",
            profileBackgroundUrl = "https://example.com/background.png",
        )
        val entitlements = CosmeticEntitlements(setOf(CosmeticEntitlement.PROFILE_BACKGROUNDS))

        assertEquals(
            ProfileBackgroundSelection.Custom("https://example.com/background.png"),
            resolveProfileBackground(profile, entitlements),
        )
    }
}
