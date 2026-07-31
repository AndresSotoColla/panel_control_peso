package com.example.panel_control_peso.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

// PALETA AJUSTADA EN EL GRÁFICO
val ChartBgWhite = Color(0xFFFFFFFF)
val ChartBorderSoft = Color(0xFFE5DDD0)
val ChartLineDarkBeige = Color(0xFFB47B48) // Beige oscuro cálido
val ChartTextCharcoal = Color(0xFF18181B)
val ChartGridGray = Color(0xFFE5E5E5)
val ChartDotBeige = Color(0xFF8C5829)

@Composable
fun GrowthCurveChart(
    series: List<WeightSeriesEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ChartBgWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChartBorderSoft),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Curva de Crecimiento (Peso vs. Edad en Meses)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ChartTextCharcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Eje Y: Peso Promedio (g) | Eje X: Edad de la Planta (Meses)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF52525B)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (series.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Sin datos históricos para graficar", color = Color(0xFF52525B), fontSize = 12.sp)
                }
            } else {
                val sortedSeries = series.sortedBy { it.edadMeses }

                val maxWeight = (sortedSeries.maxOfOrNull { it.pesoPromedio } ?: 1000.0).coerceAtLeast(100.0)
                val minWeight = (sortedSeries.minOfOrNull { it.pesoPromedio } ?: 0.0).coerceAtMost(maxWeight - 10)

                val maxAge = (sortedSeries.maxOfOrNull { it.edadMeses } ?: 12.0).coerceAtLeast(1.0)
                val minAge = (sortedSeries.minOfOrNull { it.edadMeses } ?: 0.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val leftPadding = 54.dp.toPx()
                    val rightPadding = 16.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 32.dp.toPx()

                    val chartWidth = size.width - leftPadding - rightPadding
                    val chartHeight = size.height - topPadding - bottomPadding

                    val textPaint = AndroidPaint().apply {
                        color = AndroidColor.parseColor("#52525B")
                        textSize = 24f
                        isAntiAlias = true
                        textAlign = AndroidPaint.Align.RIGHT
                    }

                    val xTextPaint = AndroidPaint().apply {
                        color = AndroidColor.parseColor("#52525B")
                        textSize = 24f
                        isAntiAlias = true
                        textAlign = AndroidPaint.Align.CENTER
                    }

                    // Y-Axis Labels and Horizontal Grid Lines
                    val stepsY = 4
                    for (i in 0..stepsY) {
                        val fraction = i.toFloat() / stepsY
                        val yVal = maxWeight - fraction * (maxWeight - minWeight)
                        val yPos = topPadding + fraction * chartHeight

                        drawLine(
                            color = ChartGridGray,
                            start = Offset(leftPadding, yPos),
                            end = Offset(size.width - rightPadding, yPos),
                            strokeWidth = 1.dp.toPx()
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.1fg", yVal),
                            leftPadding - 8.dp.toPx(),
                            yPos + 8f,
                            textPaint
                        )
                    }

                    // X-Axis Labels and Points Calculation
                    val points = mutableListOf<Offset>()
                    val ageSpan = (maxAge - minAge).coerceAtLeast(0.1)
                    val weightSpan = (maxWeight - minWeight).coerceAtLeast(1.0)

                    sortedSeries.forEachIndexed { index, entry ->
                        val xFraction = ((entry.edadMeses - minAge) / ageSpan).toFloat()
                        val xPos = leftPadding + xFraction * chartWidth

                        val yFraction = ((maxWeight - entry.pesoPromedio) / weightSpan).toFloat()
                        val yPos = topPadding + yFraction * chartHeight

                        points.add(Offset(xPos, yPos))

                        // Draw X-axis label (plant age in months) for specific intervals
                        if (sortedSeries.size <= 8 || index % (sortedSeries.size / 6).coerceAtLeast(1) == 0) {
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format(Locale.US, "%.1fm", entry.edadMeses),
                                xPos,
                                size.height - 4.dp.toPx(),
                                xTextPaint
                            )
                        }
                    }

                    // Draw Smooth Growth Curve Line
                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val p0 = points[i - 1]
                                val p1 = points[i]
                                val controlX = (p0.x + p1.x) / 2f
                                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = ChartLineDarkBeige,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Draw Point Markers on line
                    points.forEach { pt ->
                        drawCircle(
                            color = ChartDotBeige,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = ChartBgWhite,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
