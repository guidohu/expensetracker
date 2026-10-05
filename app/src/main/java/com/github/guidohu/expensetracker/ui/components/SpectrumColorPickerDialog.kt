package com.github.guidohu.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Hand-rolled HSV picker (no color-picker dependency needed): a saturation/value square for the
 * current hue, a hue strip below it, and a hex field for precise entry. Returns a plain ARGB Int
 * so it drops straight into [com.github.guidohu.expensetracker.data.Category.color].
 */
@Composable
fun SpectrumColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    val initialHsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor or 0xFF000000.toInt(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf(argbToHex(currentArgb(hue, saturation, brightness))) }
    var hexError by remember { mutableStateOf(false) }

    fun syncHexFromHsv() {
        hexText = argbToHex(currentArgb(hue, saturation, brightness))
        hexError = false
    }

    val currentColor = Color(currentArgb(hue, saturation, brightness))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom color") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(hue) {
                            fun updateFromOffset(offset: Offset) {
                                saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                brightness = (1f - offset.y / size.height).coerceIn(0f, 1f)
                                syncHexFromHsv()
                            }
                            detectTapGestures { updateFromOffset(it) }
                        }
                        .pointerInput(hue) {
                            detectDragGestures { change, _ ->
                                saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                brightness = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                syncHexFromHsv()
                            }
                        }
                        .background(
                            Brush.horizontalGradient(listOf(Color.White, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))))
                        )
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                ) {
                    SatValueIndicator(saturation = saturation, brightness = brightness)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            fun updateHue(x: Float) {
                                hue = (x / size.width * 360f).coerceIn(0f, 359.999f)
                                syncHexFromHsv()
                            }
                            detectTapGestures { updateHue(it.x) }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ -> run {
                                hue = (change.position.x / size.width * 360f).coerceIn(0f, 359.999f)
                                syncHexFromHsv()
                            } }
                        }
                        .background(Brush.horizontalGradient(HUE_GRADIENT_STOPS))
                ) {
                    HueIndicator(hue = hue)
                }

                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                    )
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { text ->
                            hexText = text
                            val parsed = parseHex(text)
                            if (parsed != null) {
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed, hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                                hexError = false
                            } else {
                                hexError = true
                            }
                        },
                        label = { Text("Hex") },
                        isError = hexError,
                        singleLine = true,
                        modifier = Modifier.padding(start = 12.dp).fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentArgb(hue, saturation, brightness)) }) { Text("Select") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SatValueIndicator(saturation: Float, brightness: Float) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        val center = Offset(saturation * size.width, (1f - brightness) * size.height)
        drawCircle(Color.White, radius = 10.dp.toPx(), center = center, style = Stroke(width = 2.dp.toPx()))
        drawCircle(Color.Black, radius = 8.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
private fun HueIndicator(hue: Float) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
        val x = (hue / 360f) * size.width
        drawCircle(Color.White, radius = size.height / 2, center = Offset(x, size.height / 2), style = Stroke(width = 3.dp.toPx()))
    }
}

private val HUE_GRADIENT_STOPS = listOf(
    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
)

private fun currentArgb(hue: Float, saturation: Float, brightness: Float): Int =
    android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)) or 0xFF000000.toInt()

private fun argbToHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

private fun parseHex(text: String): Int? {
    val cleaned = text.removePrefix("#")
    if (cleaned.length != 6) return null
    val rgb = cleaned.toIntOrNull(16) ?: return null
    return rgb or 0xFF000000.toInt()
}
