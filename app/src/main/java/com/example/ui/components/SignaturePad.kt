package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.i18n.tr
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate900
import java.io.ByteArrayOutputStream

data class SignatureStroke(
    val points: List<Offset>
)

class SignaturePadState {
    internal val strokes = mutableStateListOf<SignatureStroke>()
    internal var activePoints by mutableStateOf<List<Offset>>(emptyList())
    internal var canvasSize by mutableStateOf(IntSize(900, 320))
    internal var strokeRevision by mutableIntStateOf(0)

    val hasSignature: Boolean
        get() = strokes.isNotEmpty() || activePoints.isNotEmpty()

    fun clear() {
        strokes.clear()
        activePoints = emptyList()
        strokeRevision++
    }

    fun exportToBase64Png(): String? {
        if (!hasSignature) return null
        val srcW = canvasSize.width.coerceAtLeast(300)
        val srcH = canvasSize.height.coerceAtLeast(140)
        val outW = 900
        val outH = 320
        val scaleX = outW.toFloat() / srcW.toFloat()
        val scaleY = outH.toFloat() / srcH.toFloat()
        val scale = minOf(scaleX, scaleY)
        val offsetX = (outW - srcW * scale) / 2f
        val offsetY = (outH - srcH * scale) / 2f

        val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)

        val paint = AndroidPaint().apply {
            color = AndroidColor.parseColor("#0F172A")
            isAntiAlias = true
            strokeWidth = 6.5f
            style = AndroidPaint.Style.STROKE
            strokeJoin = AndroidPaint.Join.ROUND
            strokeCap = AndroidPaint.Cap.ROUND
        }

        strokes.forEach { stroke ->
            if (stroke.points.size == 1) {
                val p = stroke.points.first()
                canvas.drawPoint(p.x * scale + offsetX, p.y * scale + offsetY, paint)
            } else if (stroke.points.size > 1) {
                val path = AndroidPath()
                val first = stroke.points.first()
                path.moveTo(first.x * scale + offsetX, first.y * scale + offsetY)
                for (i in 1 until stroke.points.size) {
                    val prev = stroke.points[i - 1]
                    val curr = stroke.points[i]
                    val midX = ((prev.x + curr.x) / 2f) * scale + offsetX
                    val midY = ((prev.y + curr.y) / 2f) * scale + offsetY
                    path.quadTo(
                        prev.x * scale + offsetX,
                        prev.y * scale + offsetY,
                        midX,
                        midY
                    )
                }
                val last = stroke.points.last()
                path.lineTo(last.x * scale + offsetX, last.y * scale + offsetY)
                canvas.drawPath(path, paint)
            }
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        return "data:image/png;base64,$base64"
    }
}

@Composable
fun rememberSignaturePadState(): SignaturePadState {
    return remember { SignaturePadState() }
}

@Composable
fun SignaturePad(
    state: SignaturePadState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Draw,
                    contentDescription = tr(R.string.sig_draw_below),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = tr(R.string.sig_draw_below),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(
                onClick = { state.clear() },
                enabled = state.hasSignature,
                modifier = Modifier.testTag("clear_signature_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = tr(R.string.sig_clear),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(tr(R.string.sig_clear))
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Slate200, RoundedCornerShape(12.dp))
                .testTag("signature_canvas"),
            color = Color.White,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!state.hasSignature) {
                    Text(
                        text = tr(R.string.sig_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { state.canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    state.activePoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    state.activePoints = state.activePoints + change.position
                                },
                                onDragEnd = {
                                    if (state.activePoints.isNotEmpty()) {
                                        state.strokes.add(SignatureStroke(state.activePoints))
                                        state.activePoints = emptyList()
                                        state.strokeRevision++
                                    }
                                },
                                onDragCancel = {
                                    state.activePoints = emptyList()
                                }
                            )
                        }
                ) {
                    // Baseline guide
                    val baselineY = size.height * 0.78f
                    drawLine(
                        color = Slate200,
                        start = Offset(24.dp.toPx(), baselineY),
                        end = Offset(size.width - 24.dp.toPx(), baselineY),
                        strokeWidth = 1.dp.toPx()
                    )

                    val strokeStyle = Stroke(
                        width = 3.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )

                    fun drawPoints(points: List<Offset>) {
                        if (points.size == 1) {
                            drawCircle(Slate900, radius = 2.dp.toPx(), center = points.first())
                        } else if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    val prev = points[i - 1]
                                    val curr = points[i]
                                    quadraticTo(
                                        prev.x,
                                        prev.y,
                                        (prev.x + curr.x) / 2f,
                                        (prev.y + curr.y) / 2f
                                    )
                                }
                                lineTo(points.last().x, points.last().y)
                            }
                            drawPath(path = path, color = Slate900, style = strokeStyle)
                        }
                    }

                    state.strokes.forEach { drawPoints(it.points) }
                    if (state.activePoints.isNotEmpty()) {
                        drawPoints(state.activePoints)
                    }
                }
            }
        }
    }
}

@Composable
fun Base64SignaturePreview(
    dataUrl: String,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, dataUrl) {
        value = withContext(Dispatchers.Default) {
            try {
                val base64Part = dataUrl.substringAfter("base64,", dataUrl)
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    bitmap?.let { decodedBitmap ->
        Surface(
            modifier = modifier,
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = tr(R.string.sig_preview_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(8.dp)
            )
        }
    }
}
