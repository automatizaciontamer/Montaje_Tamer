package com.example.montaje_tamer.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * Máscara para Horarios (HH:mm) con desplazamiento de derecha a izquierda.
 * Ejemplo: "1" -> "00:01", "123" -> "01:23", "1234" -> "12:34"
 */
class TimeTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        val trimmed = if (digits.length >= 4) digits.substring(digits.length - 4) else digits
        val padded = trimmed.padStart(4, '0')
        
        val out = "${padded.substring(0, 2)}:${padded.substring(2, 4)}"

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = out.length
            override fun transformedToOriginal(offset: Int): Int = text.length
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

/**
 * Máscara para Moneda ($ #.###,##) con desplazamiento de derecha a izquierda.
 * Ejemplo: "123" -> "$ 1,23", "123456" -> "$ 1.234,56"
 */
class CurrencyTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val symbols = DecimalFormatSymbols(Locale("es", "AR")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val df = DecimalFormat("$ #,##0.00", symbols)

        val digits = text.text.filter { it.isDigit() }
        val number = if (digits.isEmpty()) 0.0 else digits.toDouble() / 100.0
        val out = df.format(number)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = out.length
            override fun transformedToOriginal(offset: Int): Int = text.length
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
