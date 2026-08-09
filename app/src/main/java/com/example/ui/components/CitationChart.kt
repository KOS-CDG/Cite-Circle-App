package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Spacing
import com.example.ui.theme.eyebrow
import com.example.ui.theme.numeric

@Composable
fun CitationChart(
    dataPoints: List<Float> = listOf(10f, 15f, 30f, 25f, 40f, 60f, 85f, 110f),
    hIndex: Int = 24,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "CITATION IMPACT",
                    style = MaterialTheme.typography.eyebrow,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "h-index: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$hIndex",
                        style = MaterialTheme.typography.numeric,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.base))

            val lineColor = MaterialTheme.colorScheme.primary
            val fillTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (dataPoints.size < 2) return@Canvas
                val maxPoint = dataPoints.maxOrNull()?.takeIf { it > 0f } ?: 1f
                val stepX = size.width / (dataPoints.size - 1)

                val line = Path()
                dataPoints.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = size.height - ((value / maxPoint) * size.height)
                    if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
                }

                // Area fill under the curve, fading out toward the baseline.
                val area = Path().apply {
                    addPath(line)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = area,
                    brush = Brush.verticalGradient(
                        colors = listOf(fillTop, Color.Transparent),
                        startY = 0f,
                        endY = size.height,
                    ),
                )

                drawPath(path = line, color = lineColor, style = Stroke(width = 2.dp.toPx()))

                dataPoints.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = size.height - ((value / maxPoint) * size.height)
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }
        }
    }
}
