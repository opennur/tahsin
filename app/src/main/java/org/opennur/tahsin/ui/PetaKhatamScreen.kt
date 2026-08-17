package org.opennur.tahsin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonSize
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.util.JuzStatusRow
import org.opennur.tahsin.util.KhatamStatus
import org.opennur.tahsin.util.PageStatusRow

private const val GRID_COLUMNS = 28

@Composable
fun PetaKhatamScreen(
    onBack: () -> Unit,
    onOpenPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PetaKhatamViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    LaunchedEffect(viewModel) { viewModel.refreshLanguage() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AyahColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // ---- Header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(text = "\u2190", variant = AyahButtonVariant.Outline, onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            AyahText(
                strings.petaTitle,
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            strings.petaSubtitle,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.loading -> AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(strings.statsLoading, style = AyahTypography.Body2)
            }
            state.pageStatuses.isEmpty() -> AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(strings.petaNoData, style = AyahTypography.Body2)
            }
            else -> {
                // ---- Summary ----
                PetaKhatamSummaryCard(state, strings)
                Spacer(modifier = Modifier.height(12.dp))

                // ---- Legend ----
                PetaKhatamLegend(strings)
                Spacer(modifier = Modifier.height(12.dp))

                // ---- View toggle ----
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AyahButton(
                        text = strings.petaViewPages,
                        variant = if (state.viewMode == "pages") AyahButtonVariant.Primary
                        else AyahButtonVariant.Outline,
                        size = AyahButtonSize.Small,
                        onClick = { viewModel.setViewMode("pages") },
                    )
                    AyahButton(
                        text = strings.petaViewJuz,
                        variant = if (state.viewMode == "juz") AyahButtonVariant.Primary
                        else AyahButtonVariant.Outline,
                        size = AyahButtonSize.Small,
                        onClick = { viewModel.setViewMode("juz") },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // ---- Grid / Juz circles ----
                if (state.viewMode == "pages") {
                    PetaKhatamPageGrid(state.pageStatuses, onOpenPage)
                } else {
                    PetaKhatamJuzCircles(state.juzStatuses, state.juzStartPages, onOpenPage)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PetaKhatamSummaryCard(
    state: PetaKhatamUiState,
    strings: Strings,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AyahText(
                strings.petaSummary.format(state.summary.percentGood),
                style = AyahTypography.Heading1.copy(color = AyahColors.Primary),
            )
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                "${state.summary.goodPages} / ${state.summary.totalPages}",
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
        }
    }
}

@Composable
private fun PetaKhatamLegend(strings: Strings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        LegendItem(color = AyahColors.Success, label = strings.petaLegendGood)
        LegendItem(color = AyahColors.Reading, label = strings.petaLegendReview)
        LegendItem(color = AyahColors.SurfaceVariant, label = strings.petaLegendUntouched)
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(4.dp))
        AyahText(
            label,
            style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PetaKhatamPageGrid(
    pages: List<PageStatusRow>,
    onOpenPage: (Int) -> Unit,
) {
    val rows = pages.chunked(GRID_COLUMNS)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (rowPages in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (page in rowPages) {
                    val color = when (page.status) {
                        KhatamStatus.GOOD -> AyahColors.Success
                        KhatamStatus.NEEDS_REVIEW -> AyahColors.Reading
                        KhatamStatus.UNTOUCHED -> AyahColors.SurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, RoundedCornerShape(1.dp))
                            .border(0.5.dp, AyahColors.Hairline, RoundedCornerShape(1.dp))
                            .clickable { onOpenPage(page.page) },
                    )
                }
                // Fill remaining space in last row
                repeat(GRID_COLUMNS - rowPages.size) {
                    Box(modifier = Modifier.size(10.dp))
                }
            }
        }
    }
    // Page count label
    Spacer(modifier = Modifier.height(8.dp))
    AyahText(
        "${pages.size} halaman",
        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PetaKhatamJuzCircles(
    juzList: List<JuzStatusRow>,
    startPages: Map<Int, Int>,
    onOpenPage: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (juz in juzList) {
            val color = when (juz.status) {
                KhatamStatus.GOOD -> AyahColors.Success
                KhatamStatus.NEEDS_REVIEW -> AyahColors.Reading
                KhatamStatus.UNTOUCHED -> AyahColors.SurfaceVariant
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, AyahColors.Hairline, CircleShape)
                    .clickable {
                        onOpenPage(startPages[juz.juz] ?: 1)
                    },
            ) {
                AyahText(
                    "${juz.juz}",
                    style = AyahTypography.Body1.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}
