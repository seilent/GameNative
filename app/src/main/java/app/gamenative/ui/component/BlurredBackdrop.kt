package app.gamenative.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.gamenative.ui.theme.Motion
import app.gamenative.ui.theme.PluviaBackground
import app.gamenative.ui.theme.PluviaPurple
import app.gamenative.ui.util.GameAccent
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class BackdropData(val image: ImageBitmap, val accent: Color)

/**
 * App-wide glassmorphism backdrop: a heavily blurred, scaled game thumbnail behind the UI.
 *
 * Generalized from the old `LibraryDynamicBackdrop`. The image is decoded ONCE at ~160x90 (cheap),
 * used to both (a) extract a per-game accent from the unblurred pixels and (b) produce an all-API
 * box-blurred backdrop. Crossfades with the calm [Motion.BackdropCrossfade] curve and layers scrims
 * so translucent glass content stays legible. Falls back to a soft radial gradient when no art.
 *
 * @param imageModel the art to show (URL string / Coil model), or null for the gradient fallback.
 * @param accentKey stable key (e.g. appId) used to cache the extracted accent.
 * @param onAccent invoked with the extracted (or fallback) accent color.
 */
@Composable
fun BlurredBackdrop(
    imageModel: Any?,
    modifier: Modifier = Modifier,
    accentKey: String? = null,
    blurRadius: Int = 16,
    onAccent: (Color) -> Unit = {},
) {
    val context = LocalContext.current

    val data by produceState<BackdropData?>(initialValue = null, imageModel, accentKey) {
        val model = imageModel
        if (model == null) {
            value = null
            return@produceState
        }
        delay(60)
        value = withContext(Dispatchers.IO) {
            runCatching {
                val cacheKey = model.toString()
                val cachedBlur = GameAccent.cachedBlur(cacheKey)
                if (cachedBlur != null) {
                    val fallbackArgb = PluviaPurple.toArgb()
                    val accentArgb = accentKey?.let { GameAccent.cachedArgb(it) } ?: fallbackArgb
                    return@runCatching BackdropData(cachedBlur.asImageBitmap(), Color(accentArgb))
                }

                val request = ImageRequest.Builder(context)
                    .data(model)
                    .size(160, 90)
                    .allowHardware(false)
                    .build()
                val bitmap = (context.imageLoader.execute(request) as? SuccessResult)
                    ?.drawable
                    ?.toBitmap()
                    ?: return@runCatching null

                val fallbackArgb = PluviaPurple.toArgb()
                val accentArgb = accentKey?.let { GameAccent.cachedArgb(it) }
                    ?: GameAccent.accentFromBitmap(bitmap, fallbackArgb).also { argb ->
                        accentKey?.let { GameAccent.put(it, argb) }
                    }

                val blurred = GameAccent.boxBlur(bitmap, blurRadius)
                GameAccent.putBlur(cacheKey, blurred)
                BackdropData(blurred.asImageBitmap(), Color(accentArgb))
            }.getOrNull()
        }
    }

    LaunchedEffect(data?.accent) {
        data?.accent?.let(onAccent)
    }

    var base by remember { mutableStateOf<BackdropData?>(null) }
    var incoming by remember { mutableStateOf<BackdropData?>(null) }
    val incomingAlpha = remember { Animatable(0f) }
    LaunchedEffect(data) {
        val target = data
        when {
            target == null -> {
                base = null
                incoming = null
                incomingAlpha.snapTo(0f)
            }
            base == null -> {
                base = target
                incoming = null
                incomingAlpha.snapTo(0f)
            }
            target != base -> {
                incoming = target
                incomingAlpha.snapTo(0f)
                incomingAlpha.animateTo(1f, animationSpec = Motion.BackdropCrossfade)
                base = target
                incoming = null
                incomingAlpha.snapTo(0f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PluviaBackground),
    ) {
        val baseData = base
        if (baseData != null) {
            Image(
                bitmap = baseData.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.15f
                        scaleY = 1.15f
                    },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1A1025), PluviaBackground),
                        ),
                    ),
            )
        }

        val incomingData = incoming
        if (incomingData != null) {
            Image(
                bitmap = incomingData.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.15f
                        scaleY = 1.15f
                        alpha = incomingAlpha.value
                    },
            )
        }

        // Scrim keeps glass content legible over bright art.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f)),
        )
    }
}
