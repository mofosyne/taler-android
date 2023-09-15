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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountInputField(
    value: String,
    onValueChange: (value: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.outlinedShape,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    val decimalSeparator = DecimalFormat().decimalFormatSymbols.decimalSeparator
    var tmpIn by remember { mutableStateOf(value) }

    // React to external changes
    val tmpOut = remember(tmpIn, value) {
        transformOutput(tmpIn, decimalSeparator, '.').let {
            if (value != it) value else tmpIn
        }
    }

    OutlinedTextField(
        value = tmpOut,
        onValueChange = { input ->
            val filtered = transformOutput(input, decimalSeparator, '.')
            if (Amount.isValidAmountStr(filtered)) {
                tmpIn = transformInput(input, decimalSeparator, '.')
                // tmpIn = input
                onValueChange(filtered)
            }
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle.copy(fontFamily = FontFamily.Monospace),
        label = label,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = AmountInputVisualTransformation(decimalSeparator),
        keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Decimal),
        keyboardActions = keyboardActions,
        singleLine = true,
        maxLines = 1,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
    )
}

private class AmountInputVisualTransformation(
    private val decimalSeparator: Char,
): VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val value = text.text
        val output = transformOutput(value, '.', decimalSeparator)
        val newText = AnnotatedString(output)
        return TransformedText(newText, CursorOffsetMapping(
            unmaskedText = text.toString(),
            maskedText = newText.toString().replace(decimalSeparator, '.'),
        ))
    }

    private class CursorOffsetMapping(
        private val unmaskedText: String,
        private val maskedText: String,
    ): OffsetMapping {
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
        Column {
            Text(modifier = Modifier.padding(16.dp), text = value)
            AmountInputField(
                value = value,
                onValueChange = { value = it },
            )
        }
    }
}