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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
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
    val qrPlaceHolder = if (LocalInspectionMode.current) {
        QrCodeManager.makeQrCode(talerUri, qrCodeSize.value.toInt()).asImageBitmap()
    } else null
    val qrState = produceState(qrPlaceHolder) {
        value = QrCodeManager.makeQrCode(talerUri, qrCodeSize.value.toInt()).asImageBitmap()
    }
    qrState.value?.let { qrCode ->
        Card(
            modifier = Modifier.padding(8.dp),
            shape = ShapeDefaults.Medium,
        ) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .size(qrCodeSize)
                    .align(CenterHorizontally),
                bitmap = qrCode,
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(id = R.string.button_scan_qr_code),
            )
        }
    }
    if (inBetween != null) {
        Spacer(modifier = Modifier.height(16.dp))
        inBetween()
    }
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.padding(16.dp)) {
        Text(
            modifier = Modifier.horizontalScroll(scrollState),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyLarge,
            text = talerUri,
        )
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CopyToClipboardButton(
            label = clipBoardLabel,
            content = talerUri,
            buttonText = buttonText,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        ShareButton(
            content = talerUri,
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
        Icon(
            Icons.Default.ContentCopy,
            buttonText,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(buttonText)
    }
}

fun copyToClipBoard(context: Context, label: String, str: String) {
    val clipboard = context.getSystemService<ClipboardManager>()
    val clip = ClipData.newPlainText(label, str)
    clipboard?.setPrimaryClip(clip)
}
