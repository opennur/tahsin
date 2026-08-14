package org.opennur.tahsin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.opennur.tahsin.theme.AyahColors
import kotlin.math.roundToInt

/**
 * Slider custom — aplikasi sengaja TANPA material3, jadi slider dibangun
 * sendiri: track + bagian terisi + thumb bulat yang bisa digeret (thumb ikut
 * melompat saat disentuh di posisi tertentu). Dipakai untuk ukuran huruf
 * mushaf (Tahsin) dan kecepatan pemutaran audio (Pengaturan).
 */
@Composable
fun AyahSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
    val thumbPx = with(LocalDensity.current) { 20.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .height(28.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { p ->
                        onValueChange(sliderValueAtX(p.x, size.width.toFloat(), thumbPx, min, max))
                    },
                    onDrag = { change, _ ->
                        onValueChange(sliderValueAtX(change.position.x, size.width.toFloat(), thumbPx, min, max))
                        change.consume()
                    },
                )
            },
    ) {
        val trackWidth = with(LocalDensity.current) { maxWidth.toPx() }
        val thumbCenter = thumbPx / 2f + fraction * (trackWidth - thumbPx)
        // Rel (latar track).
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AyahColors.SurfaceVariant),
        )
        // Bagian terisi.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AyahColors.Primary),
        )
        // Thumb.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((thumbCenter - thumbPx / 2f).roundToInt(), 0) }
                .size(20.dp)
                .background(AyahColors.Primary, CircleShape)
                .border(2.dp, AyahColors.Background, CircleShape),
        )
    }
}

/** Peta posisi x (px) di track ke nilai slider (min..max); thumb menempel ujung. */
fun sliderValueAtX(x: Float, width: Float, thumbSize: Float, min: Float, max: Float): Float {
    if (width <= thumbSize) return min
    val usable = width - thumbSize
    val f = ((x - thumbSize / 2f) / usable).coerceIn(0f, 1f)
    return min + f * (max - min)
}
