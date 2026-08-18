package org.opennur.tahsin.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography

/** Indikator loading kustom (tanpa Material 3). */
@Composable
fun AyahLoadingView(
    modifier: Modifier = Modifier,
    message: String = "Memuat…",
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loadingAlpha",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AyahText(
            "🕋",
            style = TextStyle(fontSize = 40.sp, color = AyahColors.TextPrimary),
            modifier = Modifier.alpha(alpha),
        )
        Spacer(modifier = Modifier.height(12.dp))
        AyahText(
            message,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
    }
}

/** Tampilan error + tombol retry kustom. */
@Composable
fun AyahErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "🔄 Coba Lagi",
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AyahText(
            "⚠️",
            style = TextStyle(fontSize = 40.sp, color = AyahColors.TextPrimary),
        )
        Spacer(modifier = Modifier.height(12.dp))
        AyahText(
            message,
            style = AyahTypography.Body1.copy(
                color = AyahColors.TextSecondary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AyahButton(text = retryLabel, onClick = onRetry)
    }
}

/** Progress bar determinate (tanpa Material 3) untuk target harian dll. */
@Composable
fun GoalProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(AyahColors.SurfaceVariant, RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(8.dp)
                .background(AyahColors.Primary, RoundedCornerShape(4.dp)),
        )
    }
}
