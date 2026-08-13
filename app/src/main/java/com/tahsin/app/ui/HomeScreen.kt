package com.tahsin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahShapes
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahText
import com.tahsin.app.ui.components.CreditLink

/**
 * Portal layar utama: grid kartu menu untuk semua fitur.
 *
 * Tiap menu membuka layarnya sendiri (di-push di atas Home di MainActivity) —
 * tanpa drawer. Bahasa & mode gelap mengikuti [SettingsUiState] bersama.
 */
@Composable
fun HomeScreen(
    onOpenTahsin: () -> Unit,
    onOpenVocab: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAudioManager: () -> Unit,
    onOpenDreamBig: () -> Unit,
    onOpenSettings: () -> Unit,
    settings: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    val strings = AppStrings.of(settings.language)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AyahColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(strings.appTitle, style = AyahTypography.Heading1)
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            strings.homeSubtitle,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Baris 1: Tahsin (utama, disorot) + Kosakata
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMenuCard(
                text = strings.menuTahsin,
                onClick = onOpenTahsin,
                highlighted = true,
                modifier = Modifier.weight(1f),
            )
            HomeMenuCard(
                text = strings.menuVocab,
                onClick = onOpenVocab,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Baris 2: Kuis Tajwid + Statistik
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMenuCard(
                text = strings.quizTitle,
                onClick = onOpenQuiz,
                modifier = Modifier.weight(1f),
            )
            HomeMenuCard(
                text = strings.menuStats,
                onClick = onOpenStats,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Baris 3: Pencarian + Kelola Audio
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMenuCard(
                text = strings.searchTitle,
                onClick = onOpenSearch,
                modifier = Modifier.weight(1f),
            )
            HomeMenuCard(
                text = strings.menuAudio,
                onClick = onOpenAudioManager,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Baris 4: Materi Dream BIG (penuh)
        HomeMenuCard(
            text = strings.menuDreamBig,
            onClick = onOpenDreamBig,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Baris 5: Pengaturan (penuh, disorot)
        HomeMenuCard(
            text = strings.menuSettings,
            onClick = onOpenSettings,
            highlighted = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(28.dp))
        CreditLink(
            text = strings.credit,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Kartu menu portal: label di tengah, latar sesuai varian. */
@Composable
private fun HomeMenuCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val container = if (highlighted) AyahColors.PrimarySoft else AyahColors.SurfaceVariant
    Column(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AyahText(
            text,
            style = AyahTypography.Body2.copy(
                color = AyahColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
