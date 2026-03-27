package com.formbuddy.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.formbuddy.android.R

data class SignatureStroke(val points: List<Offset>)

@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    strokes: List<SignatureStroke> = emptyList(),
    onStrokesChanged: (List<SignatureStroke>) -> Unit = {},
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 3f,
    backgroundColor: Color = Color.White,
    enabled: Boolean = true,
    placeholder: String = stringResource(R.string.signature_placeholder)
) {
    val currentStrokes = remember { mutableStateListOf<SignatureStroke>().apply { addAll(strokes) } }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val isEmpty = currentStrokes.isEmpty() && currentPoints.isEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isEmpty) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    if (currentPoints.size > 1) {
                                        currentStrokes.add(SignatureStroke(currentPoints))
                                        onStrokesChanged(currentStrokes.toList())
                                    }
                                    currentPoints = emptyList()
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            val style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // Draw completed strokes
            currentStrokes.forEach { stroke ->
                if (stroke.points.size > 1) {
                    val path = Path().apply {
                        moveTo(stroke.points[0].x, stroke.points[0].y)
                        for (i in 1 until stroke.points.size) {
                            lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                    }
                    drawPath(path, strokeColor, style = style)
                }
            }

            // Draw current stroke
            if (currentPoints.size > 1) {
                val path = Path().apply {
                    moveTo(currentPoints[0].x, currentPoints[0].y)
                    for (i in 1 until currentPoints.size) {
                        lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                }
                drawPath(path, strokeColor, style = style)
            }
        }
    }
}

fun signatureStrokesToPath(strokes: List<SignatureStroke>): String {
    if (strokes.isEmpty()) return ""
    return strokes.joinToString("|") { stroke ->
        stroke.points.joinToString(",") { "${it.x}:${it.y}" }
    }
}

fun pathToSignatureStrokes(pathString: String): List<SignatureStroke> {
    if (pathString.isBlank()) return emptyList()
    return pathString.split("|").map { strokeStr ->
        val points = strokeStr.split(",").mapNotNull { pointStr ->
            val parts = pointStr.split(":")
            if (parts.size == 2) {
                Offset(parts[0].toFloatOrNull() ?: 0f, parts[1].toFloatOrNull() ?: 0f)
            } else null
        }
        SignatureStroke(points)
    }
}
