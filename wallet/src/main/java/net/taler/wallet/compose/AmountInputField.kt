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

import android.os.Build
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
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
import net.taler.common.Amount
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

const val DEFAULT_INPUT_DECIMALS = 2

@Composable
fun AmountInputField(
    value: String,
    onValueChange: (value: String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    decimalFormatSymbols: DecimalFormatSymbols = DecimalFormat().decimalFormatSymbols,
    numberOfDecimals: Int = DEFAULT_INPUT_DECIMALS,
) {
    var amountInput by remember { mutableStateOf(value) }

    // React to external changes
    val amountValue = remember(amountInput, value) {
        transformOutput(amountInput).let {
            if (value != it) transformInput(value, numberOfDecimals) else amountInput
        }
    }

    OutlinedTextField(
        value = amountValue,
        onValueChange = { input ->
            if (input.matches("0+".toRegex())) {
                amountInput = "0"
                onValueChange("")
            } else transformOutput(input, numberOfDecimals)?.let { filtered ->
                if (Amount.isValidAmountStr(filtered) && !input.contains("-")) {
                    amountInput = input.trimStart('0')
                    onValueChange(filtered)
                }
            }
        },
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
        label = label,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = AmountInputVisualTransformation(
            symbols = decimalFormatSymbols,
            fixedCursorAtTheEnd = true,
            numberOfDecimals = numberOfDecimals,
        ),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberPassword),
        keyboardActions = keyboardActions,
        singleLine = true,
        maxLines = 1,
    )
}

// 500 -> 5.0
private fun transformOutput(
    input: String,
    numberOfDecimals: Int = 2,
) = if (input.isEmpty()) "0" else {
    input.toLongOrNull()?.let { it / 10.0.pow(numberOfDecimals) }?.toBigDecimal()?.toPlainString()
}

// 5.0 -> 500
private fun transformInput(
    output: String,
    numberOfDecimals: Int = 2,
) = if (output.isEmpty()) "0" else {
    (output.toDouble() * 10.0.pow(numberOfDecimals)).roundToLong().toString()
}

// Source: https://github.com/banmarkovic/CurrencyAmountInput

private class AmountInputVisualTransformation(
    private val symbols: DecimalFormatSymbols,
    private val fixedCursorAtTheEnd: Boolean = true,
    private val numberOfDecimals: Int = 2,
): VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val thousandsSeparator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            symbols.monetaryGroupingSeparator
        } else {
            symbols.groupingSeparator
        }
        val decimalSeparator = symbols.monetaryDecimalSeparator
        val zero = symbols.zeroDigit

        val inputText = text.text

        val intPart = inputText
            .dropLast(numberOfDecimals)
            .reversed()
            .chunked(3)
            .joinToString(thousandsSeparator.toString())
            .reversed()
            .ifEmpty {
                zero.toString()
            }

        val fractionPart = inputText.takeLast(numberOfDecimals).let {
            if (it.length != numberOfDecimals) {
                List(numberOfDecimals - it.length) {
                    zero
                }.joinToString("") + it
            } else {
                it
            }
        }

        // Hide trailing decimal separator if decimals are 0
        val formattedNumber = if (numberOfDecimals > 0) {
            intPart + decimalSeparator + fractionPart
        } else {
            intPart
        }

        val newText = AnnotatedString(
            text = formattedNumber,
            spanStyles = text.spanStyles,
            paragraphStyles = text.paragraphStyles
        )

        val offsetMapping = if (fixedCursorAtTheEnd) {
            FixedCursorOffsetMapping(
                contentLength = inputText.length,
                formattedContentLength = formattedNumber.length
            )
        } else {
            MovableCursorOffsetMapping(
                unmaskedText = text.toString(),
                maskedText = newText.toString(),
                decimalDigits = numberOfDecimals
            )
        }

        return TransformedText(newText, offsetMapping)
    }

    private class FixedCursorOffsetMapping(
        private val contentLength: Int,
        private val formattedContentLength: Int,
    ) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = formattedContentLength
        override fun transformedToOriginal(offset: Int): Int = contentLength
    }

    private class MovableCursorOffsetMapping(
        private val unmaskedText: String,
        private val maskedText: String,
        private val decimalDigits: Int
    ) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
            when {
                unmaskedText.length <= decimalDigits -> {
                    maskedText.length - (unmaskedText.length - offset)
                }
                else -> {
                    offset + offsetMaskCount(offset, maskedText)
                }
            }

        override fun transformedToOriginal(offset: Int): Int =
            when {
                unmaskedText.length <= decimalDigits -> {
                    max(unmaskedText.length - (maskedText.length - offset), 0)
                }
                else -> {
                    offset - maskedText.take(offset).count { !it.isDigit() }
                }
            }

        private fun offsetMaskCount(offset: Int, maskedText: String): Int {
            var maskOffsetCount = 0
            var dataCount = 0
            for (maskChar in maskedText) {
                if (!maskChar.isDigit()) {
                    maskOffsetCount++
                } else if (++dataCount > offset) {
                    break
                }
            }
            return maskOffsetCount
        }
    }
}