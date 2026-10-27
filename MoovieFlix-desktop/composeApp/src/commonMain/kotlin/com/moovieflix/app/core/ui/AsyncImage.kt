package com.moovieflix.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter

internal enum class MoovieFlixDesktopImageScaling {
    Auto,
    Disabled,
}

/** Desktop only: how animated GIF / WebP artwork plays. Other platforms already animate. */
enum class MoovieFlixDesktopAnimation {
    /** Show the first frame. */
    Off,

    /** Show the first frame until the pointer is over the image, then play. */
    OnHover,

    /** Play whenever the image is on screen. */
    Always,
}

@Composable
internal expect fun MoovieFlixAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallback: Painter? = error,
    onLoading: ((AsyncImagePainter.State.Loading) -> Unit)? = null,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality? = null,
    clipToBounds: Boolean = true,
    desktopImageScaling: MoovieFlixDesktopImageScaling = MoovieFlixDesktopImageScaling.Auto,
    imageLoader: ImageLoader? = null,
    /** Desktop only: playback mode for animated GIF / WebP. */
    desktopAnimation: MoovieFlixDesktopAnimation = MoovieFlixDesktopAnimation.Off,
)
