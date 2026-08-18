package org.opennur.tahsin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahShapes
import org.opennur.tahsin.theme.AyahTypography

/** Satu opsi dropdown kustom (tanpa Material 3). */
data class DropdownOption(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * Dropdown kustom (tanpa Material 3): trigger berupa kotak yang bisa diketuk,
 * isinya daftar opsi yang bisa di-scroll di dalam Popup.
 */
@Composable
fun SimpleDropdown(
    selectedLabel: String,
    options: List<DropdownOption>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var triggerHeight by remember { mutableStateOf(0) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .onSizeChanged { triggerHeight = it.height }
                .clip(AyahShapes.Field)
                .background(AyahColors.SurfaceVariant)
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics {
                    this.contentDescription = selectedLabel
                    this.role = Role.Button
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            AyahText(
                "$selectedLabel ▾",
                style = AyahTypography.Body2.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                ),
                // Maks 2 baris — kalau label kepanjangan, wrap (TIDAK "...").
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, triggerHeight),
                onDismissRequest = { expanded = false },
            ) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .background(AyahColors.Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, AyahColors.Hairline, RoundedCornerShape(12.dp)),
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(options, key = { it.label }) { option ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable(role = Role.Button) {
                                        expanded = false
                                        option.onClick()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                AyahText(option.label, style = AyahTypography.Body2)
                            }
                        }
                    }
                }
            }
        }
    }
}
