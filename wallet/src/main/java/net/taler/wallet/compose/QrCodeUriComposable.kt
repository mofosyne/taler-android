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

package net.taler.wallet.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.content.getSystemService
import net.taler.common.QrCodeManager
import net.taler.wallet.R

@Composable
fun ColumnScope.QrCodeUriComposable(
    talerUri: String,
    clipBoardLabel: String,
    buttonText: String = stringResource(R.string.copy),
    inBetween: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val qrCodeSize = getQrCodeSize()
    val qrState = produceState<ImageBitmap?>(null) {
        value = QrCodeManager.makeQrCode(talerUri, qrCodeSize.value.toInt()).asImageBitmap()
    }
    qrState.value?.let { qrCode ->
        Image(
            modifier = Modifier.size(qrCodeSize),
            bitmap = qrCode,
            contentDescription = stringResource(id = R.string.button_scan_qr_code),
        )
    }
    if (inBetween != null) inBetween()
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.padding(16.dp)) {
        Text(
            modifier = Modifier.horizontalScroll(scrollState),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.body1,
            text = talerUri,
        )
    }
    CopyToClipboardButton(
        modifier = Modifier,
        label = clipBoardLabel,
        content = talerUri,
        buttonText = buttonText,
    )
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
) {
    val context = LocalContext.current
    Button(
        modifier = modifier,
        onClick = { copyToClipBoard(context, label, content) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCopy, stringResource(R.string.copy))
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = buttonText,
                style = MaterialTheme.typography.body1,
            )
        }
    }
}

fun copyToClipBoard(context: Context, label: String, str: String) {
    val clipboard = context.getSystemService<ClipboardManager>()
    val clip = ClipData.newPlainText(label, str)
    clipboard?.setPrimaryClip(clip)
}