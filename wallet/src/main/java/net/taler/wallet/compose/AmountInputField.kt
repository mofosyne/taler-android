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

package net.taler.wallet.compose

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import java.text.DecimalFormat

@Composable
fun AmountInputField(
    value: String,
    onValueChange: (value: String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val decimalSeparator = DecimalFormat().decimalFormatSymbols.decimalSeparator
    var amountInput by remember { mutableStateOf(value) }

    // React to external changes
    val amountValue = remember(amountInput, value) {
        transformOutput(amountInput, decimalSeparator, '.').let {
            if (value != it) value else amountInput
        }
    }

    OutlinedTextField(
        value = amountValue,
        onValueChange = { input ->
            val filtered = transformOutput(input, decimalSeparator, '.')
            if (Amount.isValidAmountStr(filtered)) {
                amountInput = transformInput(input, decimalSeparator, '.')
                // tmpIn = input
                onValueChange(filtered)
            }
        },
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
        label = label,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = AmountInputVisualTransformation(decimalSeparator),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
        keyboardActions = keyboardActions,
        singleLine = true,
        maxLines = 1,
    )
}

private class AmountInputVisualTransformation(
    private val decimalSeparator: Char,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val value = text.text
        val output = transformOutput(value, '.', decimalSeparator)
        val newText = AnnotatedString(output)
        return TransformedText(
            newText, CursorOffsetMapping(
                unmaskedText = text.toString(),
                maskedText = newText.toString().replace(decimalSeparator, '.'),
            )
        )
    }

    private class CursorOffsetMapping(
        private val unmaskedText: String,
        private val maskedText: String,
    ) : OffsetMapping {
        override fun originalToTransformed(offset: Int) = when {
            unmaskedText.startsWith('.') -> if (offset == 0) 0 else (offset + 1) // ".x" -> "0.x"
            else -> offset
        }

        override fun transformedToOriginal(offset: Int) = when {
            unmaskedText == "" -> 0 // "0" -> ""
            unmaskedText == "." -> if (offset < 1) 0 else 1 // "0.0" -> "."
            unmaskedText.startsWith('.') -> if (offset < 1) 0 else (offset - 1) // "0.x" -> ".x"
            unmaskedText.endsWith('.') && offset == maskedText.length -> offset - 1 // "x.0" -> "x."
            else -> offset // "x" -> "x"
        }
    }
}

private fun transformInput(
    input: String,
    inputDecimalSeparator: Char = '.',
    outputDecimalSeparator: Char = '.',
) = input.trim().replace(inputDecimalSeparator, outputDecimalSeparator)

private fun transformOutput(
    input: String,
    inputDecimalSeparator: Char = '.',
    outputDecimalSeparator: Char = '.',
) = transformInput(input, inputDecimalSeparator, outputDecimalSeparator).let {
    when {
        it.isEmpty() -> "0"
        it == "$outputDecimalSeparator" -> "0${outputDecimalSeparator}0"
        it.startsWith(outputDecimalSeparator) -> "0$it"
        it.endsWith(outputDecimalSeparator) -> "${it}0"
        else -> it
    }
}

@Preview
@Composable
fun AmountInputFieldPreview() {
    var value by remember { mutableStateOf("0") }
    TalerSurface {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = spacedBy(16.dp),
        ) {
            AmountInputField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Amount input:") },
                supportingText = { Text("This amount is nice.") },
            )
            AmountInputField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Error in amount input:") },
                supportingText = { Text("Amount is invalid.") },
                isError = true,
            )
        }
    }
}
