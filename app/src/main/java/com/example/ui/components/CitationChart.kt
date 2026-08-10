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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Placeholder citation history. Not derived from any real data source. */
private val sampleCitationHistory = listOf(10f, 15f, 30f, 25f, 40f, 60f, 85f, 110f)
private const val SAMPLE_H_INDEX = "24"

/** Sparkline of citation impact over time, shown on the profile. */
@Composable
fun CitationChart(
    modifier: Modifier = Modifier,
    dataPoints: List<Float> = sampleCitationHistory,
    hIndex: String = SAMPLE_H_INDEX,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = CiteCircleDefaults.CardShape,
        border = CiteCircleDefaults.cardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("CITATION IMPACT")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "h-index: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        hIndex,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val lineColor = MaterialTheme.colorScheme.secondary
            val chartDescription = "Citation impact trend, h-index $hIndex, " +
                "${dataPoints.size} periods rising from " +
                "${dataPoints.firstOrNull()?.toInt() ?: 0} to " +
                "${dataPoints.lastOrNull()?.toInt() ?: 0} citations"

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = chartDescription },
            ) {
                if (dataPoints.isEmpty()) return@Canvas

                val path = Path()
                val maxPoint = dataPoints.maxOrNull()?.takeIf { it > 0f } ?: 1f
                val stepX = size.width / (dataPoints.size - 1).coerceAtLeast(1)

                dataPoints.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = size.height - ((value / maxPoint) * size.height)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

/** Compact stat pill used alongside the chart on profile screens. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
