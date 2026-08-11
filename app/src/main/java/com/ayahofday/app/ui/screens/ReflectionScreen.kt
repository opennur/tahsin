package com.ayahofday.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ayahofday.app.data.sample.SampleData
import com.ayahofday.app.theme.AyahColors
import com.ayahofday.app.theme.AyahShapes
import com.ayahofday.app.theme.AyahTypography
import com.ayahofday.app.ui.components.AyahButton
import com.ayahofday.app.ui.components.AyahCard
import com.ayahofday.app.ui.components.AyahText

/**
 * Tab 3 — Reflection: pelajaran (lessons learned), amalan praktis,
 * dan jurnal harian yang nantinya disimpan ke Room.
 */
@Composable
fun ReflectionScreen(
    modifier: Modifier = Modifier,
) {
    var journalText by remember { mutableStateOf("") }
    // TODO: ganti state lokal dengan JournalDao (Room) via VerseRepository.observeJournals()
    var savedJournals by remember { mutableStateOf<List<String>>(emptyList()) }

    fun saveJournal() {
        val text = journalText.trim()
        if (text.isEmpty()) return
        savedJournals = listOf(text) + savedJournals
        journalText = ""
        // TODO: simpan ke Room via VerseRepository.saveJournal(Journal(content = text))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        AyahText("✍️ Refleksi", style = AyahTypography.Heading1)
        AyahText(
            "Renungkan ayat hari ini, tulis pelajaran dan amalanmu.",
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Lessons learned ----
        AyahText("Pelajaran Hari Ini", style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(8.dp))
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            SampleData.lessonsLearned.forEach { lesson ->
                AyahText("•  $lesson", style = AyahTypography.Body1)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Amalan praktis ----
        AyahText("Amalan Praktis", style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(8.dp))
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            SampleData.practicalDeeds.forEachIndexed { index, deed ->
                AyahText(
                    "${index + 1}.  $deed",
                    style = AyahTypography.Body1,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Jurnal harian ----
        AyahText("Jurnal Harian", style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(8.dp))
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = journalText,
                onValueChange = { journalText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AyahColors.Background, AyahShapes.Button)
                    .padding(12.dp),
                textStyle = AyahTypography.Body1.copy(color = AyahColors.TextPrimary),
                cursorBrush = SolidColor(AyahColors.Primary),
                minLines = 3,
                decorationBox = { innerTextField ->
                    Box {
                        if (journalText.isEmpty()) {
                            AyahText(
                                "Tulis refleksimu di sini…",
                                style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary),
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            AyahButton(
                text = "💾 Simpan Refleksi",
                onClick = { saveJournal() },
                enabled = journalText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ---- Jurnal tersimpan ----
        if (savedJournals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            AyahText("Jurnal Tersimpan", style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            savedJournals.forEach { journal ->
                AyahCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    AyahText(
                        journal,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
