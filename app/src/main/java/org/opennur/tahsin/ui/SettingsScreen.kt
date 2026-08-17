package org.opennur.tahsin.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import org.opennur.tahsin.ui.components.AyahCard
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
data class SettingsAppearanceActions(
    val onToggleTajwidColor: () -> Unit,
    val onToggleTranslation: () -> Unit,
    val onToggleDarkMode: () -> Unit,
    val onSetLanguage: (AppLanguage) -> Unit,
    val onEditLearningPlan: () -> Unit,
)

data class SettingsAudioActions(
    val onSetReciter: (Reciter) -> Unit,
    val onSetSpeed: (Float) -> Unit,
)

data class SettingsNotificationActions(
    val onToggleAyahOfDay: () -> Unit,
    val onToggleStreakReminder: () -> Unit,
)

data class SettingsActions(
    val appearance: SettingsAppearanceActions,
    val audio: SettingsAudioActions,
    val notifications: SettingsNotificationActions,
    val onDownloadAll: () -> Unit,
    val onOpenAudioManager: () -> Unit,
    val onDataImported: () -> Unit = {},
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settings: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strings = AppStrings.of(settings.language)

    val backupViewModel: BackupViewModel = viewModel()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(backupState.importCompleted) {
        if (backupState.importCompleted) actions.onDataImported()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { backupViewModel.exportTo(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { backupViewModel.importFrom(it) }
    }

    // Izin notifikasi (Android 13+) diminta saat user menghidupkan notifikasi harian.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* keputusan dipakai otomatis oleh postNotification */ }
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
            SettingsAppearanceSection(settings, strings, actions.appearance)

            SectionDivider()

            SettingsAudioSection(settings, strings, actions.audio)

            SectionDivider()

            SettingsDownloadSection(settings, strings, actions)

            SectionDivider()

            SettingsDataSection(strings, backupState, onExport = {
                exportLauncher.launch("tahsin-backup.json")
            }, onImport = {
                importLauncher.launch(arrayOf("application/json"))
            }, onDismissMessage = { backupViewModel.clearMessage() })

            SectionDivider()

            SettingsNotificationSection(
                settings = settings,
                strings = strings,
                actions = actions.notifications,
                requestNotificationPermission = requestNotificationPermission,
            )

            SectionDivider()
            CreditLink(text = strings.credit)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsAppearanceSection(
    settings: SettingsUiState,
    strings: Strings,
    actions: SettingsAppearanceActions,
) {
    val languageName = if (settings.language == AppLanguage.ID) strings.languageNameId else strings.languageNameEn
    SectionLabel(strings.sectionAppearance)
    Spacer(modifier = Modifier.height(6.dp))
    SettingRow("🎨 ${strings.settingTajwid}", settings.tajwidColor, actions.onToggleTajwidColor)
    SettingRow("🌐 ${strings.tahsinTranslation}", settings.showTranslation, actions.onToggleTranslation)
    SettingRow("🌙 ${strings.settingDarkMode}", settings.darkMode, actions.onToggleDarkMode)
    SettingRow(
        label = "🌐 ${strings.settingLanguage}",
        value = languageName,
        onClick = { actions.onSetLanguage(settings.language.next()) },
    )
    Spacer(modifier = Modifier.height(8.dp))
    AyahButton(
        text = strings.settingLearningPlan,
        variant = AyahButtonVariant.Outline,
        onClick = actions.onEditLearningPlan,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingsAudioSection(
    settings: SettingsUiState,
    strings: Strings,
    actions: SettingsAudioActions,
) {
    SectionLabel(strings.sectionReciter)
    Spacer(modifier = Modifier.height(6.dp))
    SimpleDropdown(
        selectedLabel = settings.reciter.label,
        options = Reciter.entries.map { r -> DropdownOption(r.label) { actions.onSetReciter(r) } },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    SectionLabel(strings.sectionSpeed)
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        AyahSlider(
            value = settings.audioSpeed,
            onValueChange = actions.onSetSpeed,
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
}

@Composable
private fun SettingsDownloadSection(
    settings: SettingsUiState,
    strings: Strings,
    actions: SettingsActions,
) {
    SectionLabel(strings.sectionMenu)
    Spacer(modifier = Modifier.height(8.dp))
    AyahButton(
        text = strings.menuAudio,
        variant = AyahButtonVariant.Outline,
        onClick = actions.onOpenAudioManager,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    AyahButton(
        text = strings.menuDownloadAll,
        variant = if (settings.isDownloading) AyahButtonVariant.Outline else AyahButtonVariant.Primary,
        onClick = actions.onDownloadAll,
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
}

@Composable
private fun SettingsNotificationSection(
    settings: SettingsUiState,
    strings: Strings,
    actions: SettingsNotificationActions,
    requestNotificationPermission: () -> Unit,
) {
    SettingRow(
        label = "🗓️ ${strings.sectionDaily}",
        checked = settings.ayahOfDayEnabled,
        onCheckedChange = {
            if (!settings.ayahOfDayEnabled) requestNotificationPermission()
            actions.onToggleAyahOfDay()
        },
    )
    SettingRow(
        label = "🔥 ${strings.sectionStreakReminder}",
        checked = settings.streakReminderEnabled,
        onCheckedChange = {
            if (!settings.streakReminderEnabled) requestNotificationPermission()
            actions.onToggleStreakReminder()
        },
    )
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

@Composable
private fun SettingsDataSection(
    strings: Strings,
    backupState: BackupUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    SectionLabel(strings.sectionData)
    Spacer(modifier = Modifier.height(8.dp))
    if (backupState.message != null) {
        AyahCard(modifier = Modifier.fillMaxWidth(), onClick = onDismissMessage) {
            AyahText(
                backupState.message ?: "",
                style = AyahTypography.Body2.copy(
                    color = if (backupState.success) AyahColors.Success else AyahColors.Error,
                ),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    AyahButton(
        text = strings.exportBackup,
        variant = AyahButtonVariant.Outline,
        onClick = onExport,
        enabled = !backupState.busy,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    AyahButton(
        text = strings.importBackup,
        variant = AyahButtonVariant.Outline,
        onClick = onImport,
        enabled = !backupState.busy,
        modifier = Modifier.fillMaxWidth(),
    )
}
