/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
 *
 * GNU Taler is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3, or (at your option) any later version.
 *
 * GNU Taler is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GNU Taler; see the file COPYING.  If not, see <http://www.gnu.org/licenses/>
 */

package net.taler.anastasis.ui.reusable.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.content.getSystemService
import net.taler.anastasis.R
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.common.QrCodeManager

@Composable
fun ColumnScope.QrCode(
    uri: String,
    clipBoardLabel: String,
    buttonText: String = stringResource(R.string.copy),
    inBetween: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val qrCodeSize = getQrCodeSize()
    val qrCode = QrCodeManager.makeQrCode(uri, qrCodeSize.value.toInt()).asImageBitmap()
    Image(
        modifier = Modifier
            .size(qrCodeSize)
            .align(CenterHorizontally),
        bitmap = qrCode,
        contentDescription = stringResource(id = R.string.button_scan_qr_code),
    )
    if (inBetween != null) inBetween()
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.padding(vertical = LocalSpacing.current.medium)) {
        Text(
            modifier = Modifier.horizontalScroll(scrollState),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyLarge,
            text = uri,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CopyToClipboardButton(
            label = clipBoardLabel,
            content = uri,
            buttonText = buttonText,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        ShareButton(
            content = uri,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

@Composable
fun getQrCodeSize(): Dp {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    return min(screenHeight, screenWidth)
}

@Composable
fun CopyToClipboardButton(
    label: String,
    content: String,
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(R.string.copy),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    val context = LocalContext.current
    Button(
        modifier = modifier,
        colors = colors,
        onClick = { copyToClipBoard(context, label, content) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = buttonText,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

fun copyToClipBoard(context: Context, label: String, str: String) {
    val clipboard = context.getSystemService<ClipboardManager>()
    val clip = ClipData.newPlainText(label, str)
    clipboard?.setPrimaryClip(clip)
}
