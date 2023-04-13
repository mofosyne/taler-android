/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
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

package net.taler.wallet.transactions

import android.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.getAttrColor

@Composable
// FIXME this assumes that it is used in a column and applies its own padding, not really re-usable
fun TransactionLinkComposable(label: String, info: String, onClick: () -> Unit) {
    Text(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = label,
        style = MaterialTheme.typography.bodyMedium,
    )
    val context = LocalContext.current
    val linkColor = Color(context.getAttrColor(R.attr.textColorLink))
    val annotatedString = buildAnnotatedString {
        pushStringAnnotation(tag = "url", annotation = info)
        withStyle(style = SpanStyle(color = linkColor)) {
            append(info)
        }
        pop()
    }
    ClickableText(
        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        text = annotatedString,
        style = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
    ) { offset ->
        annotatedString.getStringAnnotations(
            tag = "url",
            start = offset,
            end = offset,
        ).firstOrNull()?.let {
            onClick()
        }
    }
}

@Preview
@Composable
fun TransactionLinkComposablePreview() {
    TalerSurface {
        Column(
            horizontalAlignment = CenterHorizontally,
        ) {
            TransactionLinkComposable(
                label = "This is a label",
                info = "This is some fulfillment message"
            ) {}
        }
    }
}
