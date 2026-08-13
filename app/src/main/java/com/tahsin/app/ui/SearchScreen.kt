package com.tahsin.app.ui

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.theme.ArabicFont
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahShapes
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahCard
import com.tahsin.app.ui.components.AyahText
import com.tahsin.app.util.FontStore
import com.tahsin.app.util.SearchableAyah

/**
 * Layar pencarian ayat: cari kata Arab (ternormalisasi) atau kata kunci
 * terjemahan ID/EN di seluruh mushaf. Ketuk hasil → buka ayat di layar utama.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel(factory = searchViewModelFactory(context))
    LaunchedEffect(viewModel) { viewModel.refreshLanguage() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = remember { FontStore(context).loadFamily(ArabicFont.UTSMANI) }

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
                    strings.searchTitle,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Kolom pencarian ----
            SearchField(
                query = state.query,
                placeholder = strings.searchHint,
                onQueryChange = viewModel::setQuery,
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.query.isBlank() -> StatusCard(strings.searchNoQuery)
                state.indexing -> StatusCard(strings.searchIndexing)
                state.searching && state.results.isEmpty() -> StatusCard(strings.searchSearching)
                state.results.isEmpty() -> StatusCard(strings.searchNoResults)
                else -> {
                    AyahText(
                        strings.searchResultsCount.format(state.results.size),
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    state.results.forEach { r ->
                        SearchResultCard(
                            result = r,
                            surahName = state.surahNames[r.surahNumber] ?: "Surah ${r.surahNumber}",
                            fontFamily = arabicFamily,
                            onClick = { onOpenAyah(r.surahNumber, r.ayahNumber) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Kolom input pencarian kustom (tanpa Material): latar SurfaceVariant. */
@Composable
private fun SearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AyahShapes.Field)
            .background(AyahColors.SurfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        val keyboard = LocalSoftwareKeyboardController.current
        if (query.isEmpty()) {
            AyahText(
                placeholder,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = AyahTypography.Body2.copy(color = AyahColors.TextPrimary),
            singleLine = true,
            cursorBrush = SolidColor(AyahColors.Primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Kartu status (petunjuk / sedang memuat / tidak ada hasil). */
@Composable
private fun StatusCard(message: String) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        AyahText(
            message,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
    }
}

/** Satu hasil pencarian: surah:ayat + Arab + terjemahan ID & EN. */
@Composable
private fun SearchResultCard(
    result: SearchableAyah,
    surahName: String,
    fontFamily: FontFamily,
    onClick: () -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        AyahText(
            "$surahName :${result.ayahNumber}",
            style = AyahTypography.Caption.copy(
                color = AyahColors.Primary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(
            result.arabic,
            style = AyahTypography.Arabic.copy(
                fontSize = 18.sp,
                fontFamily = fontFamily,
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (result.translationId.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            AyahText(
                result.translationId,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (result.translationEn.isNotBlank() && result.translationEn != result.translationId) {
            Spacer(modifier = Modifier.height(2.dp))
            AyahText(
                result.translationEn,
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
