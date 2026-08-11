package com.ayahofday.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayahofday.app.data.model.Verse
import com.ayahofday.app.data.sample.SampleData
import com.ayahofday.app.theme.AyahColors
import com.ayahofday.app.theme.AyahTheme
import com.ayahofday.app.theme.AyahTypography
import com.ayahofday.app.ui.components.AyahButton
import com.ayahofday.app.ui.components.AyahButtonVariant
import com.ayahofday.app.ui.components.AyahCard
import com.ayahofday.app.ui.components.AyahText
import com.ayahofday.app.util.shareToWhatsApp

/**
 * Tab 1 — Home: ayat hari ini (Arab, transliterasi, terjemahan),
 * audio murottal, share WhatsApp, bookmark, dan navigasi ke Tafsir/Refleksi.
 */
@Composable
fun HomeScreen(
    verse: Verse = SampleData.verseOfTheDay,
    onOpenTafsir: () -> Unit,
    onOpenReflection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        AyahText("Assalamu'alaikum 👋", style = AyahTypography.Heading2)
        AyahText(
            "Ayat hari ini",
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Kartu ayat hari ini ----
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            AyahText(
                verse.reference,
                style = AyahTypography.Caption.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(verse.arabic, style = AyahTypography.Arabic)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AyahColors.Divider),
            )
            Spacer(modifier = Modifier.height(12.dp))
            AyahText(verse.transliteration, style = AyahTypography.Transliteration)
            Spacer(modifier = Modifier.height(6.dp))
            AyahText(verse.translation, style = AyahTypography.Body1)

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Audio murottal + bookmark ----
            // TODO: ganti toggle dengan pemutar murottal sungguhan
            //  (MediaPlayer/ExoPlayer dari URL audio EQuran.id).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AyahButton(
                    text = if (isPlaying) "⏸ Pause" else "▶ Murottal",
                    variant = AyahButtonVariant.Secondary,
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.weight(1f),
                )
                AyahButton(
                    text = if (isBookmarked) "⭐ Bookmarked" else "☆ Bookmark",
                    variant = AyahButtonVariant.Outline,
                    onClick = { isBookmarked = !isBookmarked },
                    modifier = Modifier.weight(1f),
                )
                // TODO: sinkronkan isBookmarked dengan BookmarkDao via VerseRepository.
            }

            Spacer(modifier = Modifier.height(12.dp))

            AyahButton(
                text = "📤 Bagikan ke WhatsApp",
                onClick = { shareToWhatsApp(context, verse.toShareText()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Kartu Tafsir ----
        AyahCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenTafsir) {
            AyahText("📖 Tafsir", style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                verse.tafsir.orEmpty(),
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                maxLines = 3,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "Baca selengkapnya →",
                style = AyahTypography.Caption.copy(color = AyahColors.Primary),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Kartu Refleksi ----
        AyahCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenReflection) {
            AyahText("✍️ Refleksi", style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "Pelajaran hari ini dan jurnal pribadimu.",
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "Tulis refleksi →",
                style = AyahTypography.Caption.copy(color = AyahColors.Primary),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AyahTheme {
        HomeScreen(onOpenTafsir = {}, onOpenReflection = {})
    }
}
