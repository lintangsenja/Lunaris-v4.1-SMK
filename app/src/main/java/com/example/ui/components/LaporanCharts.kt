package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurpleText
import java.util.Locale

// ==========================================
// 1. DONUT CHART (RINGKASAN)
// ==========================================
@Composable
fun DonutChart(
    data: Map<String, Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onSliceClick: (String) -> Unit = {}
) {
    val total = data.values.sum()
    if (data.isEmpty() || total <= 0f || total.isNaN()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Data belum tersedia", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clickable { onSliceClick("Semua") },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val strokeWidth = 24.dp.toPx()
                data.entries.forEachIndexed { index, entry ->
                    val sweepAngle = if (total > 0f) (entry.value.toFloat() / total) * 360f * animationProgress.value else 0f
                    val color = colors[index % colors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        size = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth),
                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += if (total > 0f) (entry.value.toFloat() / total) * 360f else 0f
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Stok",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${total.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = DeepPurpleText
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp)
        ) {
            data.entries.forEachIndexed { index, entry ->
                val color = colors[index % colors.size]
                val percentage = if (total > 0f) (entry.value.toDouble() / total.toDouble() * 100.0).toFloat() else 0f
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSliceClick(entry.key) }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.key}: ${entry.value.toInt()} (${String.format(Locale.US, "%.1f", percentage)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. STACKED BAR CHART (ALAT PER RUANGAN)
// ==========================================
@Composable
fun StackedBarChart(
    data: Map<String, Triple<Float, Float, Float>>,
    modifier: Modifier = Modifier,
    onBarClick: (String) -> Unit = {}
) {
    val totalSum = data.values.sumOf { (it.first + it.second + it.third).toDouble() }.toFloat()
    if (data.isEmpty() || totalSum <= 0f || totalSum.isNaN()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Data belum tersedia", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val maxTotal = data.values.maxOfOrNull { it.first + it.second + it.third } ?: 1f
    val safeMaxTotal = if (maxTotal <= 0f || maxTotal.isNaN()) 1f else maxTotal

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.entries.forEach { entry ->
                val roomName = entry.key
                val (baik, perawatan, rusak) = entry.value
                val total = baik + perawatan + rusak

                if (total > 0f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onBarClick(roomName) }
                    ) {
                        Text(
                            text = "${total.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DeepPurpleText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(130.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                                val factor = animationProgress.value / safeMaxTotal
                                val heightRusak = (rusak * factor * 130).coerceIn(0f, 130f).dp
                                val heightPerawatan = (perawatan * factor * 130).coerceIn(0f, 130f).dp
                                val heightBaik = (baik * factor * 130).coerceIn(0f, 130f).dp

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(heightRusak)
                                        .background(Color(0xFFEF4444))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(heightPerawatan)
                                        .background(Color(0xFFF59E0B))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(heightBaik)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = roomName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DeepPurpleText,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(70.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val legendItems = listOf(
                "Baik" to Color(0xFF10B981),
                "Perawatan" to Color(0xFFF59E0B),
                "Rusak" to Color(0xFFEF4444)
            )
            legendItems.forEach { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DeepPurpleText)
                }
            }
        }
    }
}

// ==========================================
// 3. LINE / AREA CHART (BAHAN TREN & ALAT RUSAK)
// ==========================================
@Composable
fun LineChart(
    data: Map<String, Float>,
    lineColor: Color = Color(0xFF3B82F6),
    modifier: Modifier = Modifier,
    onPointClick: (String) -> Unit = {}
) {
    val totalSum = data.values.sum()
    if (data.isEmpty() || totalSum <= 0f || totalSum.isNaN() || data.values.all { it == 0f }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Data belum tersedia", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val maxVal = data.values.maxOrNull() ?: 10f
    val safeMaxVal = if (maxVal <= 0f || maxVal.isNaN()) 10f else maxVal
    val keys = data.keys.toList()
    val values = data.values.toList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val width = size.width
            val height = size.height
            val numKeys = keys.size
            val spacing = if (numKeys > 1) width / (numKeys - 1) else width

            val points = values.mapIndexed { idx, value ->
                val x = if (numKeys > 1) idx * spacing else width / 2
                val y = height - (value.toFloat() / safeMaxVal) * height * 0.75f * animProgress.value - 12.dp.toPx()
                androidx.compose.ui.geometry.Offset(x, y)
            }

            // Draw grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = (height / gridLines) * i
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw filled area under the line
            val fillPath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, height)
                    points.forEach { point ->
                        lineTo(point.x, point.y)
                    }
                    lineTo(points.last().x, height)
                    close()
                }
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f))
                )
            )

            // Draw line path
            val linePath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw circles at data points
            points.forEachIndexed { index, point ->
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            keys.forEach { key ->
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepPurpleText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPointClick(key) }
                )
            }
        }
    }
}

// ==========================================
// 4. DISCIPLINE BAR CHART (PENGEMBALIAN)
// ==========================================
@Composable
fun DisciplineBarChart(
    tepatWaktu: Float,
    terlambat: Float,
    modifier: Modifier = Modifier,
    onBarClick: (String) -> Unit = {}
) {
    val total = tepatWaktu + terlambat
    if (total <= 0f || total.isNaN()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Data belum tersedia", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(tepatWaktu, terlambat) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val maxVal = maxOf(tepatWaktu, terlambat, 1f)
    val safeMaxVal = if (maxVal <= 0f || maxVal.isNaN()) 1f else maxVal

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Tepat Waktu Bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onBarClick("Tepat Waktu") }
                .padding(8.dp)
        ) {
            val pct = if (total > 0f) (tepatWaktu.toDouble() / total.toDouble() * 100.0).toFloat() else 0f
            Text(
                text = "${tepatWaktu.toInt()} (${String.format(Locale.US, "%.1f", pct)}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((120 * (tepatWaktu / safeMaxVal) * animProgress.value).coerceIn(0f, 120f)).dp)
                        .background(Color(0xFF10B981))
                        .align(Alignment.BottomCenter)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tepat Waktu", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepPurpleText)
        }

        // Terlambat Bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onBarClick("Terlambat") }
                .padding(8.dp)
        ) {
            val pct = if (total > 0f) (terlambat.toDouble() / total.toDouble() * 100.0).toFloat() else 0f
            Text(
                text = "${terlambat.toInt()} (${String.format(Locale.US, "%.1f", pct)}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((120 * (terlambat / safeMaxVal) * animProgress.value).coerceIn(0f, 120f)).dp)
                        .background(Color(0xFFEF4444))
                        .align(Alignment.BottomCenter)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Terlambat", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepPurpleText)
        }
    }
}

// ==========================================
// 5. INTERACTIVE PIE CHART (AFKIR REASONS)
// ==========================================
@Composable
fun InteractivePieChart(
    data: Map<String, Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onSliceClick: (String) -> Unit = {}
) {
    val total = data.values.sum()
    if (data.isEmpty() || total <= 0f || total.isNaN()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Data belum tersedia", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clickable { onSliceClick("Semua") }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                data.entries.forEachIndexed { index, entry ->
                    val sweepAngle = if (total > 0f) (entry.value.toFloat() / total) * 360f * animationProgress.value else 0f
                    val color = colors[index % colors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = size
                    )
                    startAngle += if (total > 0f) (entry.value.toFloat() / total) * 360f else 0f
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.entries.forEachIndexed { index, entry ->
                val color = colors[index % colors.size]
                val percentage = if (total > 0f) (entry.value.toDouble() / total.toDouble() * 100.0).toFloat() else 0f
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSliceClick(entry.key) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.key}: ${entry.value.toInt()} (${String.format(Locale.US, "%.1f", percentage)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. INTERACTIVE HORIZONTAL BAR CHART (PEMINJAMAN/MAINTENANCE)
// ==========================================
@Composable
fun InteractiveHorizontalBarChart(
    data: Map<String, Float>,
    barColor: Color,
    modifier: Modifier = Modifier,
    onBarClick: (String) -> Unit = {}
) {
    val maxVal = data.values.maxOrNull() ?: 0f
    if (data.isEmpty() || maxVal <= 0f || maxVal.isNaN()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Data belum tersedia", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val safeMaxVal = if (maxVal <= 0f || maxVal.isNaN()) 1f else maxVal

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.entries.forEach { entry ->
            val fraction = (((entry.value.toFloat() / safeMaxVal) * animProgress.value).coerceIn(0f, 1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBarClick(entry.key) }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = entry.key,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "${entry.value.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(barColor, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

// ==========================================
// 7. STOCK STATUS BAR CHART (HORIZONTAL STATUS BREAKDOWN)
// ==========================================
@Composable
fun StockStatusBarChart(
    stokAmanCount: Int,
    perluPengadaanCount: Int,
    stokKritisCount: Int,
    selectedStatus: String?,
    onBarClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(stokAmanCount, perluPengadaanCount, stokKritisCount).toFloat().coerceAtLeast(1f)
    
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(stokAmanCount, perluPengadaanCount, stokKritisCount) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val categories = listOf(
        Triple("Stok Aman", stokAmanCount, Color(0xFF10B981)), // Emerald Green
        Triple("Perlu Pengadaan", perluPengadaanCount, Color(0xFFF97316)), // Orange
        Triple("Stok Kritis", stokKritisCount, Color(0xFFEF4444)) // Red
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        categories.forEach { (label, count, color) ->
            val isSelected = selectedStatus == label
            val isAnySelected = selectedStatus != null
            val alpha = if (!isAnySelected || isSelected) 1f else 0.4f
            
            val fraction = ((count.toFloat() / maxVal) * animProgress.value).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBarClick(label) }
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color.copy(alpha = alpha))
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) color else DeepPurpleText.copy(alpha = alpha)
                        )
                    }
                    Text(
                        text = "$count Item",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color.copy(alpha = alpha)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(color.copy(alpha = 0.75f * alpha), color.copy(alpha = alpha))
                                )
                            )
                    )
                }
            }
        }
    }
}
