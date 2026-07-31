package com.example.panel_control_peso.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.panel_control_peso.data.model.WeightSeriesEntry
import java.util.Locale

val ChartBg = Color(0xFF1E293B)
val ChartLine = Color(0xFF10B981)
val ChartPoint = Color(0xFF34D399)
val ChartGrid = Color(0xFF334155)
val ChartText = Color(0xFF94A3B8)

@Composable
fun GrowthCurveChart(
    series: List<WeightSeriesEntry>,
    modifier: Modifier = Modifier
) {
    if (series.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(ChartBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Sin datos para generar curva de crecimiento.", color = ChartText, fontSize = 12.sp)
        }
        return
    }

    val maxWeight = (series.maxOfOrNull { it.pesoPromedio } ?: 100.0).coerceAtLeast(10.0)
    val minWeight = (series.minOfOrNull { it.pesoPromedio } ?: 0.0).coerceAtLeast(0.0)
    val maxAge = (series.maxOfOrNull { it.edadMeses } ?: 1.0).coerceAtLeast(0.5)
    val minAge = (series.minOfOrNull { it.edadMeses } ?: 0.0).coerceAtLeast(0.0)

    Card(
        colors = CardDefaults.cardColors(containerColor = ChartBg),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Curva de Crecimiento (Peso vs Edad)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    "X: Edad (meses) | Y: Peso (g)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChartText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(start = 46.dp, end = 16.dp, top = 12.dp, bottom = 28.dp)
            ) {
                val width = size.width
                val height = size.height

                val textPaintY = android.graphics.Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                val textPaintX = android.graphics.Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Draw Grid Lines and Y-axis Labels
                val gridLines = 4
                val weightStep = (maxWeight - minWeight) / gridLines

                for (i in 0..gridLines) {
                    val y = height * (1 - i.toFloat() / gridLines)
                    val labelVal = minWeight + (weightStep * i)

                    drawLine(
                        color = ChartGrid,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )

                    // Draw Y-axis Label Value (in grams)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${String.format(Locale.US, "%.1f", labelVal)}g",
                        -10f,
                        y + 8f,
                        textPaintY
                    )
                }

                val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)
                val ageRange = (maxAge - minAge).coerceAtLeast(0.1)

                val points = series.map { entry ->
                    val xFraction = ((entry.edadMeses - minAge) / ageRange).toFloat()
                    val yFraction = ((entry.pesoPromedio - minWeight) / weightRange).toFloat()

                    val x = xFraction * width
                    val y = height - (yFraction * height)
                    Offset(x, y) to entry
                }

                // Draw Line Path
                if (points.size > 1) {
                    val path = Path()
                    path.moveTo(points.first().first.x, points.first().first.y)
                    for (i in 1 until points.size) {
                        path.lineTo(points[i].first.x, points[i].first.y)
                    }
                    drawPath(
                        path = path,
                        color = ChartLine,
                        style = Stroke(width = 4f)
                    )
                }

                // Draw Points and X-axis Data Labels
                points.forEach { (offset, entry) ->
                    drawCircle(
                        color = ChartPoint,
                        radius = 6f,
                        center = offset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = offset
                    )

                    // Draw Age label on X-axis (in months with 1 decimal)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${String.format(Locale.US, "%.1f", entry.edadMeses)}m",
                        offset.x,
                        height + 24f,
                        textPaintX
                    )
                }
            }
        }
    }
}
