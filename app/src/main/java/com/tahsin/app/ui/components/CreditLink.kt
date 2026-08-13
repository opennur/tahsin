package com.tahsin.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography

/** Alamat repo OpenNur — kredit aplikasi. */
const val OPENNUR_URL = "https://github.com/opennur/opennur"

/**
 * Baris kredit aplikasi: menampilkan teks kredit dan membuka repo OpenNur
 * (https://github.com/opennur/opennur) saat diketuk.
 */
@Composable
fun CreditLink(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val uriHandler = LocalUriHandler.current
    AyahText(
        text,
        style = AyahTypography.Caption.copy(
            color = AyahColors.Primary,
            fontWeight = FontWeight.SemiBold,
            textAlign = textAlign,
        ),
        modifier = modifier.clickable { uriHandler.openUri(OPENNUR_URL) },
    )
}
