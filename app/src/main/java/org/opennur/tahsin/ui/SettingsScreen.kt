package org.opennur.tahsin.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahSlider
import org.opennur.tahsin.ui.components.AyahButtonSize
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahSwitch
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.ui.components.CreditLink
import org.opennur.tahsin.ui.components.DropdownOption
import org.opennur.tahsin.ui.components.SimpleDropdown
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.AudioSpeeds
import org.opennur.tahsin.util.Reciter
import org.opennur.tahsin.util.next

/**
 * Layar Pengaturan penuh — pengganti drawer: semua setelan aplikasi dalam satu
 * layar mandiri (dibuka dari portal atau ikon ⚙ di Tahsin).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settings: SettingsUiState,
    onToggleTajwidColor: () -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onEditLearningPlan: () -> Unit,
    onSetReciter: (Reciter) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleAyahOfDay: () -> Unit,
    onToggleStreakReminder: () -> Unit,
    onDownloadAll: () -> Unit,
    onOpenAudioManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strings = AppStrings.of(settings.language)
    val languageName = if (settings.language == AppLanguage.ID) strings.languageNameId else strings.languageNameEn

    // Izin notifikasi (Android 13+) diminta saat user menghidupkan notifikasi harian.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* keputusan dipakai otomatis oleh postNotification */ }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AyahColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // ---- Header: kembali + judul ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(
                text = "←",
                variant = AyahButtonVariant.Outline,
                size = AyahButtonSize.Small,
                onClick = onBack,
            )
            Spacer(modifier = Modifier.width(10.dp))
            AyahText(strings.settingsTitle, style = AyahTypography.Heading2)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // ---- Tampilan: warna tajwid, flow, mode gelap, bahasa ----
            SectionLabel(strings.sectionAppearance)
            Spacer(modifier = Modifier.height(6.dp))
            SettingRow(
                label = "🎨 ${strings.settingTajwid}",
                checked = settings.tajwidColor,
                onCheckedChange = onToggleTajwidColor,
            )
            SettingRow(
                label = "🌐 ${strings.tahsinTranslation}",
                checked = settings.showTranslation,
                onCheckedChange = onToggleTranslation,
            )
            SettingRow(
                label = "🌙 ${strings.settingDarkMode}",
                checked = settings.darkMode,
                onCheckedChange = onToggleDarkMode,
            )
            SettingRow(
                label = "🌐 ${strings.settingLanguage}",
                value = languageName,
                onClick = { onSetLanguage(settings.language.next()) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahButton(
                text = strings.settingLearningPlan,
                variant = AyahButtonVariant.Outline,
                onClick = onEditLearningPlan,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionDivider()

            // ---- Audio: qari' + kecepatan ----
            SectionLabel(strings.sectionReciter)
            Spacer(modifier = Modifier.height(6.dp))
            SimpleDropdown(
                selectedLabel = settings.reciter.label,
                options = Reciter.entries.map { r -> DropdownOption(r.label) { onSetReciter(r) } },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
            SectionLabel(strings.sectionSpeed)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahSlider(
                    value = settings.audioSpeed,
                    onValueChange = onSetSpeed,
                    modifier = Modifier.weight(1f),
                    valueRange = AudioSpeeds.MIN..AudioSpeeds.MAX,
                )
                Spacer(modifier = Modifier.width(10.dp))
                AyahText(
                    AudioSpeeds.format(settings.audioSpeed),
                    style = AyahTypography.Caption.copy(
                        color = AyahColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.width(52.dp),
                )
            }

            SectionDivider()

            // ---- Aksi: Kelola Audio (pindah dari menu utama) + Unduh Semua ----
            SectionLabel(strings.sectionMenu)
            Spacer(modifier = Modifier.height(8.dp))
            AyahButton(
                text = strings.menuAudio,
                variant = AyahButtonVariant.Outline,
                onClick = onOpenAudioManager,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahButton(
                text = strings.menuDownloadAll,
                variant = if (settings.isDownloading) AyahButtonVariant.Outline else AyahButtonVariant.Primary,
                onClick = onDownloadAll,
                enabled = !settings.isDownloading,
                modifier = Modifier.fillMaxWidth(),
            )
            if (settings.isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                AyahText(
                    "${settings.downloadDone} / ${settings.downloadTotal}",
                    style = AyahTypography.Caption.copy(color = AyahColors.Primary),
                )
            }

            SectionDivider()

            // ---- Ayah of the Day ----
            SettingRow(
                label = "🗓️ ${strings.sectionDaily}",
                checked = settings.ayahOfDayEnabled,
                onCheckedChange = {
                    // Menghidupkan notifikasi tanpa izin (API 33+) → minta izin dulu.
                    if (!settings.ayahOfDayEnabled && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onToggleAyahOfDay()
                },
            )

            // ---- Pengingat streak ----
            SettingRow(
                label = "🔥 ${strings.sectionStreakReminder}",
                checked = settings.streakReminderEnabled,
                onCheckedChange = {
                    if (!settings.streakReminderEnabled && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onToggleStreakReminder()
                },
            )

            SectionDivider()
            CreditLink(text = strings.credit)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Pembatas seksi tipis. */
@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(12.dp))
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AyahColors.Hairline))
    Spacer(modifier = Modifier.height(12.dp))
}

/** Label seksi kecil di atas grup setelan. */
@Composable
private fun SectionLabel(text: String) {
    AyahText(
        text,
        style = AyahTypography.Overline.copy(
            color = AyahColors.Primary,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

/** Baris setelan ringkas: label + saklar di kanan. Seluruh baris bisa diketuk. */
@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable(role = Role.Switch, onClick = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AyahText(
            label,
            style = AyahTypography.Body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        AyahSwitch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            interactive = false,
        )
    }
}

/** Baris setelan dengan nilai di kanan (mis. bahasa) — diketuk untuk berganti. */
@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AyahText(
            label,
            style = AyahTypography.Body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        AyahText(
            value,
            style = AyahTypography.Body2.copy(
                color = AyahColors.Primary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.width(6.dp))
        AyahText("▸", style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
    }
}
