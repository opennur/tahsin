package org.opennur.tahsin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.util.AppLanguage

/**
 * Layar Keajaiban & Keindahan Al-Qur'an: daftar kategori (ilmiah, kabar masa
 * depan, bahasa, penjagaan teks) dengan konten statis [WondersContent] yang
 * bersumber; baris sumber bisa diketuk untuk membuka tautan verifikasi.
 * Bahasa dikirim dari MainActivity (settingsState bersama, selalu segar).
 */
@Composable
fun WondersScreen(
    onBack: () -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val strings = AppStrings.of(language)
    val uriHandler = LocalUriHandler.current

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
                    strings.wondersTitle,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                strings.wondersSubtitle,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Kategori & item ----
            WondersContent.categories.forEach { category ->
                AyahText(
                    "${category.emoji} ${textOf(language, category.titleId, category.titleEn)}",
                    style = AyahTypography.Heading2.copy(color = AyahColors.Primary),
                )
                if (category.noteId.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    AyahText(
                        textOf(language, category.noteId, category.noteEn),
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                category.items.forEach { item ->
                    WonderItemCard(
                        item = item,
                        language = language,
                        onOpenSource = { uriHandler.openUri(item.sourceUrl) },
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

/** Satu kartu konten: rujukan ayat, judul, teks, dan baris sumber (tautan). */
@Composable
private fun WonderItemCard(
    item: WonderItem,
    language: AppLanguage,
    onOpenSource: () -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            AyahText(
                item.reference,
                style = AyahTypography.Caption.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            AyahText(
                textOf(language, item.titleId, item.titleEn),
                style = AyahTypography.Body2.copy(
                    color = AyahColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                textOf(language, item.textId, item.textEn),
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                textOf(language, item.sourceId, item.sourceEn),
                style = AyahTypography.Caption.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSource)
                    .padding(vertical = 4.dp),
            )
        }
    }
}
