package com.ayahofday.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayahofday.app.data.model.Verse
import com.ayahofday.app.data.sample.SampleData
import com.ayahofday.app.theme.AyahColors
import com.ayahofday.app.theme.AyahTypography
import com.ayahofday.app.ui.components.AyahCard
import com.ayahofday.app.ui.components.AyahText

/**
 * Tab 4 — Bookmarks: daftar ayat yang di-bookmark.
 * Tap → buka ayat (navigasi ke Tafsir). Tekan lama → hapus bookmark.
 */
@Composable
fun BookmarksScreen(
    onOpenVerse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: ganti dengan BookmarkDao.observeBookmarkedVerses() via VerseRepository
    var bookmarked by remember { mutableStateOf(SampleData.bookmarkedVerses) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        AyahText("⭐ Bookmark", style = AyahTypography.Heading1)
        AyahText(
            "Tekan lama untuk menghapus bookmark.",
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (bookmarked.isEmpty()) {
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(
                    "Belum ada bookmark.\nKetuk ☆ Bookmark pada ayat di Home untuk menyimpan.",
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            }
        } else {
            bookmarked.forEach { verse ->
                BookmarkItem(
                    verse = verse,
                    onClick = onOpenVerse, // TODO: buka ayat terpilih (nav args "tafsir/{surah}/{ayah}")
                    onLongClick = {
                        // TODO: hapus dari Room via VerseRepository.removeBookmark(verse)
                        bookmarked = bookmarked - verse
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BookmarkItem(
    verse: Verse,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    AyahCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                AyahText(
                    verse.reference,
                    style = AyahTypography.Caption.copy(
                        color = AyahColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AyahText(
                    verse.arabic,
                    style = AyahTypography.Arabic.copy(fontSize = 20.sp, lineHeight = 32.sp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AyahText(
                    verse.translation,
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AyahText(
                "⭐",
                style = TextStyle(fontSize = 18.sp, color = AyahColors.TextPrimary),
            )
        }
    }
}
