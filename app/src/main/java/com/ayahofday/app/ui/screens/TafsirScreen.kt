package com.ayahofday.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayahofday.app.data.model.Verse
import com.ayahofday.app.data.sample.SampleData
import com.ayahofday.app.theme.AyahColors
import com.ayahofday.app.theme.AyahTypography
import com.ayahofday.app.ui.components.AyahCard
import com.ayahofday.app.ui.components.AyahText

/**
 * Tab 2 — Tafsir: tafsir ayat Bahasa Indonesia (EQuran.id / Kemenag)
 * dan Asbabun Nuzul bila tersedia.
 */
@Composable
fun TafsirScreen(
    verse: Verse = SampleData.verseOfTheDay,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        AyahText("📖 Tafsir", style = AyahTypography.Heading1)
        AyahText(
            "Tafsir Bahasa Indonesia — bersumber dari EQuran.id / Kemenag.",
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Kutipan ayat yang sedang ditafsirkan
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
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(verse.translation, style = AyahTypography.Body1)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tafsir ayat
        AyahText("Tafsir", style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(8.dp))
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            // TODO: ambil tafsir dari VerseRepository.getTafsir() (EQuran.id / Kemenag)
            AyahText(verse.tafsir.orEmpty(), style = AyahTypography.Body1)
        }

        // Asbabun Nuzul (hanya jika tersedia)
        verse.asbabunNuzul?.let { asbabun ->
            Spacer(modifier = Modifier.height(16.dp))
            AyahText("Asbabun Nuzul", style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(asbabun, style = AyahTypography.Body1)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
