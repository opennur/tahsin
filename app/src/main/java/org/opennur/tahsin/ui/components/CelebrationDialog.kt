package org.opennur.tahsin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.AppStrings
import org.opennur.tahsin.ui.Strings
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.CelebrationEvent
import org.opennur.tahsin.util.CelebrationType

/**
 * Dialog perayaan gamification (naik level / streak / badge baru) —
 * dirender global di MainActivity, memakai getar sebagai umpan haptic.
 */
@Composable
fun CelebrationDialog(
    event: CelebrationEvent,
    strings: Strings,
    language: AppLanguage,
    onDismiss: () -> Unit,
) {
    val title: String
    val body: String
    when (event.type) {
        CelebrationType.LEVEL_UP -> {
            title = strings.celebrateLevelUpTitle
            body = strings.celebrateLevelUpBody.format(event.level)
        }
        CelebrationType.STREAK_MILESTONE -> {
            title = strings.celebrateStreakTitle
            body = strings.celebrateStreakBody.format(event.streak)
        }
        CelebrationType.BADGE_EARNED -> {
            title = strings.celebrateBadgeTitle
            body = strings.celebrateBadgeBody.format(
                AppStrings.badgeTitle(event.badgeKey, language),
                event.tier,
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(AyahColors.Surface)
                .padding(20.dp),
        ) {
            AyahText(
                title,
                style = AyahTypography.Heading2.copy(
                    color = AyahColors.Primary,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                body,
                style = AyahTypography.Body2.copy(
                    color = AyahColors.TextSecondary,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                AyahButton(text = strings.gotIt, variant = AyahButtonVariant.Primary, onClick = onDismiss)
            }
        }
    }
}
