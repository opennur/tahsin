package org.opennur.tahsin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.util.AppLanguage

/**
 * Layar Studi Coherence (Nazm) Al-Qur'an: tema pusat Al-Qur'an, struktur 7
 * kelompok (Farahi–Islahi), lalu untuk SETIAP surah: topik utama (amud) dan
 * keterkaitan antar surah (nazm). Konten statis [CoherenceContent] dua bahasa.
 * Bahasa dikirim dari MainActivity (settingsState bersama, selalu segar).
 */
@Composable
fun CoherenceScreen(
    onBack: () -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val strings = AppStrings.of(language)

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ---- Header ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahButton(text = "←", variant = AyahButtonVariant.Outline, onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                AyahText(
                    strings.coherenceTitle,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                strings.coherenceSubtitle,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Tema pusat Al-Qur'an ----
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    AyahText(
                        strings.coherenceCentralTheme,
                        style = AyahTypography.Body2.copy(
                            color = AyahColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AyahText(
                        textOf(
                            language,
                            CoherenceContent.centralThemeId,
                            CoherenceContent.centralThemeEn,
                        ),
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AyahText(
                        strings.coherenceMethodology,
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ---- 7 kelompok + surah per kelompok ----
            CoherenceContent.groups.forEach { group ->
                AyahText(
                    "${strings.coherenceGroupLabel.format(group.number)} · ${textOf(language, group.rangeId, group.rangeEn)}",
                    style = AyahTypography.Heading2.copy(color = AyahColors.Primary),
                )
                Spacer(modifier = Modifier.height(2.dp))
                AyahText(
                    textOf(language, group.themeId, group.themeEn),
                    style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                )
                Spacer(modifier = Modifier.height(8.dp))
                CoherenceContent.surahs
                    .filter { it.group == group.number }
                    .forEach { surah ->
                        CoherenceSurahCard(
                            surah = surah,
                            language = language,
                            nazmLabel = strings.coherenceNazmLabel,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                Spacer(modifier = Modifier.height(14.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Pilih teks sesuai bahasa (ID default, EN saat bahasa Inggris). */
private fun textOf(language: AppLanguage, id: String, en: String): String =
    if (language == AppLanguage.EN) en else id

/** Satu kartu surah: nomor + nama, topik utama (amud), keterkaitan (nazm). */
@Composable
private fun CoherenceSurahCard(
    surah: CoherenceSurah,
    language: AppLanguage,
    nazmLabel: String,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            AyahText(
                "${surah.number}. ${surah.name}",
                style = AyahTypography.Body2.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                textOf(language, surah.amudId, surah.amudEn),
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(6.dp))
            AyahText(
                "$nazmLabel: ${textOf(language, surah.nazmId, surah.nazmEn)}",
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
            )
        }
    }
}
