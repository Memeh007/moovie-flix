package com.moovieflix.tv.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import com.moovieflix.tv.ui.theme.MoovieFlixFocus
import com.moovieflix.tv.ui.theme.MoovieFlixMotion

@OptIn(ExperimentalFoundationApi::class)
object MoovieFlixScrollDefaults {
    val smoothScrollSpec = object : BringIntoViewSpec {
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override val scrollAnimationSpec: AnimationSpec<Float> = spring(
            dampingRatio = 0.95f,
            stiffness = MoovieFlixMotion.tokens.durations.fast.toFloat()
        )

        override fun calculateScrollDistance(
            offset: Float,
            size: Float,
            containerSize: Float
        ): Float {
            if (containerSize <= 0f || size <= 0f) return 0f
            val itemCenter = offset + size / 2f
            val viewportTarget = containerSize * MoovieFlixFocus.tokens.scrollViewportTarget
            val distance = itemCenter - viewportTarget
            if (offset >= 0f && offset + size <= containerSize && distance > 0f && distance < 12f) {
                return 0f
            }
            return distance
        }
    }
}
