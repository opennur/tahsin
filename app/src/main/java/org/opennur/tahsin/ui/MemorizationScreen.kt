package org.opennur.tahsin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText

@Composable
fun MemorizationScreen(
    onBack: () -> Unit,
    onOpenAyah: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MemorizationViewModel = viewModel()
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(text = "←", variant = AyahButtonVariant.Outline, onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            AyahText(
                strings.memorizationTitle,
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            strings.memorizationSubtitle,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.loading -> AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(strings.memorizationLoading, style = AyahTypography.Body2)
            }
            state.error || state.card == null || state.ayah == null -> AyahCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                AyahText(strings.memorizationError, style = AyahTypography.Body2)
            }
            else -> {
                val card = state.card ?: return
                val ayah = state.ayah ?: return
                AyahText(
                    strings.memorizationDue.format(state.dueCount, state.totalCount),
                    style = AyahTypography.Caption.copy(
                        color = AyahColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(10.dp))
                AyahCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        AyahText(
                            "${card.surah}:${card.ayah}",
                            style = AyahTypography.Caption.copy(color = AyahColors.Primary),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.revealed) {
                            AyahText(
                                ayah.text,
                                style = AyahTypography.Arabic.copy(textAlign = TextAlign.End),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AyahText(
                                ayah.translation,
                                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                            )
                        } else {
                            AyahText(
                                strings.memorizationHidden,
                                style = AyahTypography.Body1.copy(
                                    color = AyahColors.TextSecondary,
                                    textAlign = TextAlign.Center,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AyahButton(
                                text = strings.memorizationReveal,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = viewModel::reveal,
                            )
                        }
                    }
                }

                if (state.revealed) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AyahButton(
                            text = strings.memorizationReview,
                            variant = AyahButtonVariant.Danger,
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::needReview,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AyahButton(
                            text = strings.memorizationRemembered,
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::remember,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                AyahButton(
                    text = strings.memorizationOpenTahsin,
                    variant = AyahButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenAyah(card.surah, card.ayah) },
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
