package com.tahsin.app.ui

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
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonSize
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahSwitch
import com.tahsin.app.ui.components.AyahText
import com.tahsin.app.ui.components.CreditLink
import com.tahsin.app.ui.components.DropdownOption
import com.tahsin.app.ui.components.SimpleDropdown
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.AudioSpeeds
import com.tahsin.app.util.Reciter
import com.tahsin.app.util.next

/**
 * Layar Pengaturan penuh — pengganti drawer: semua setelan aplikasi dalam satu
 * layar mandiri (dibuka dari portal atau ikon ⚙ di Tahsin).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settings: SettingsUiState,
    onToggleTajwidColor: () -> Unit,
    onToggleFlowMode: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetReciter: (Reciter) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleAyahOfDay: () -> Unit,
    onToggleStreakReminder: () -> Unit,
    onDownloadAll: () -> Unit,
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
                label = "🔁 ${strings.settingFlow}",
                checked = settings.flowMode,
                onCheckedChange = onToggleFlowMode,
            )
            Spacer(modifier = Modifier.height(2.dp))
            AyahText(
                strings.flowHint,
                style = AyahTypography.Caption,
            )
            Spacer(modifier = Modifier.height(4.dp))
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AudioSpeeds.options.forEach { speed ->
                    AyahButton(
                        text = AudioSpeeds.format(speed),
                        variant = if (settings.audioSpeed == speed) {
                            AyahButtonVariant.Primary
                        } else {
                            AyahButtonVariant.Outline
                        },
                        size = AyahButtonSize.Small,
                        onClick = { onSetSpeed(speed) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SectionDivider()

            // ---- Unduh Semua (aksi) ----
            SectionLabel(strings.sectionMenu)
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
