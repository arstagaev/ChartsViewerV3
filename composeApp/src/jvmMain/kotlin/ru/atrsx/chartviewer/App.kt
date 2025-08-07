package ru.atrsx.chartviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.awt.FileDialog
import java.io.File
import ru.atrsx.chartviewer.koala.ExperimentalKoalaPlotApi
import ru.atrsx.chartviewer.koala.gestures.GestureConfig
import ru.atrsx.chartviewer.koala.line.LinePlot
import ru.atrsx.chartviewer.koala.style.LineStyle
import ru.atrsx.chartviewer.koala.xygraph.Point
import ru.atrsx.chartviewer.koala.xygraph.XYGraph
import ru.atrsx.chartviewer.koala.xygraph.autoScaleXRange
import ru.atrsx.chartviewer.koala.xygraph.autoScaleYRange
import ru.atrsx.chartviewer.koala.xygraph.rememberFloatLinearAxisModel

@Composable
@Preview
fun App() {
    var data1 by remember { mutableStateOf<ChartData?>(null) }
    var data2 by remember { mutableStateOf<ChartData?>(null) }
    var data3 by remember { mutableStateOf<ChartData?>(null) }
    var visibility1 by remember { mutableStateOf(List(8) { true }) }
    var visibility2 by remember { mutableStateOf(List(8) { true }) }
    var visibility3 by remember { mutableStateOf(List(8) { true }) }
    var path1 by remember { mutableStateOf<String?>(null) }
    var path2 by remember { mutableStateOf<String?>(null) }
    var path3 by remember { mutableStateOf<String?>(null) }


    val seriesColors = listOf(
        Color.Blue, Color.Red, Color.Green, Color.Magenta,
        Color.Cyan, Color.Yellow, Color.Gray, Color.Black
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // File load buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FileButton("File 1", path1, listOf(path1, path2, path3)) { full ->

                parseChartFile(full)?.let { data1 = it; visibility1 = it.visibility; path1 = full }
            }
            FileButton("File 2", path2, listOf(path1, path2, path3)) { full ->
                parseChartFile(full)?.let { data2 = it; visibility2 = it.visibility; path2 = full }
            }
            FileButton("File 3", path3, listOf(path1, path2, path3)) { full ->
                parseChartFile(full)?.let { data3 = it; visibility3 = it.visibility; path3 = full }
            }
        }

        // Display loaded file paths
        path1?.let { Text("File 1: $it", fontSize = 12.sp) }
        path2?.let { Text("File 2: $it", fontSize = 12.sp) }
        path3?.let { Text("File 3: $it", fontSize = 12.sp) }

        // Chart area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val datasets = listOfNotNull(data1, data2, data3)
            val visibilityStates = listOf(visibility1, visibility2, visibility3)
            if (datasets.isNotEmpty()) {
                ChartView(datasets, visibilityStates, seriesColors)
            }
        }

        // Visibility toggles for each file
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleSeriesButtons(visibility1, data1?.series, seriesColors) { visibility1 = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleSeriesButtons(visibility2, data2?.series, seriesColors) { visibility2 = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleSeriesButtons(visibility3, data3?.series, seriesColors) { visibility3 = it }
        }
    }
}

@Composable
fun FileButton(
    label: String,
    currentPath: String?,
    existingPaths: List<String?>,
    onFileSelected: (String) -> Unit
) {
    Button(onClick = {
        FileDialog(null as java.awt.Frame?, "Select $label", FileDialog.LOAD).apply {
            isVisible = true
            file?.let { fn ->
                // `this` is the FileDialog instance, so use its `directory` + `file` properties:
                val dir = this.directory
                val full = if (dir.endsWith(File.separator)) "$dir$fn" else "$dir${File.separator}$fn"
                if (existingPaths.contains(full)) return@let
                onFileSelected(full)
            }
        }
    }) { Text(label) }
}

@Composable
fun ToggleSeriesButtons(
    visibility: List<Boolean>,
    seriesList: List<List<Pair<Double, Double>>>?,
    colors: List<Color>,
    onChange: (List<Boolean>) -> Unit
) {
    seriesList?.forEachIndexed { idx, series ->
        if (series.isNotEmpty()) {
            val bg = if (visibility[idx]) colors[idx] else colors[idx].copy(alpha = 0.3f)
            Box(
                Modifier
                    .background(bg, RoundedCornerShape(4.dp))
                    .clickable { onChange(visibility.toMutableList().apply { this[idx] = !this[idx] }) }
                    .padding(8.dp)
            ) {
                Text(
                    "S$idx: ${if (visibility[idx]) "On" else "Off"}",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Parses the chart file and returns ChartData or null.
 */
fun parseChartFile(path: String): ChartData? {
    return try {
        val lines = File(path).bufferedReader().useLines { it.toList() }
        if (lines.size < 2) return null
        val visibility = lines[1].split("#").drop(2).mapNotNull { it.toIntOrNull() }.map { it == 1 }.take(8)
        val dataLines = lines.dropWhile { it.startsWith("#") }
        val series = List(8) { mutableListOf<Pair<Double, Double>>() }
        dataLines.forEach { line ->
            line.split("|").mapNotNull { seg ->
                seg.split(";").takeIf { it.size == 2 }?.let { (x, y) -> x.toDoubleOrNull()?.let { xv -> y.toDoubleOrNull()?.let { yv -> xv to yv } } }
            }.forEachIndexed { i, p -> if (i < series.size) series[i].add(p) }
        }
        ChartData(series, visibility)
    } catch (e: Exception) {
        e.printStackTrace(); null
    }
}

data class ChartData(
    val series: List<List<Pair<Double, Double>>>,
    val visibility: List<Boolean>
)

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun ChartView(
    datasets: List<ChartData>,
    visibilityStates: List<List<Boolean>>,
    colors: List<Color>
) {
    // Gather visible points
    val points = datasets.flatMapIndexed { di, cd ->
        cd.series.flatMapIndexed { si, s -> if (visibilityStates[di][si]) s.map { Point(it.first.toFloat(), it.second.toFloat()) } else emptyList() }
    }
    val xRange = points.takeIf { it.isNotEmpty() }?.autoScaleXRange() ?: (0f..1f)
    val yRange = points.takeIf { it.isNotEmpty() }?.autoScaleYRange() ?: (0f..1f)
    val xSpan = xRange.endInclusive - xRange.start
    val ySpan = yRange.endInclusive - yRange.start

    val xModel = rememberFloatLinearAxisModel(
        range = xRange,
        minViewExtent = xSpan * 0.01f,
        maxViewExtent = xSpan,
        minimumMajorTickIncrement = xSpan * 0.005f,
        minimumMajorTickSpacing = 30.dp,
        minorTickCount = 4,
    )
    val yModel = rememberFloatLinearAxisModel(
        range = yRange,
        minViewExtent = ySpan * 0.01f,
        maxViewExtent = ySpan,
        minimumMajorTickIncrement = ySpan * 0.005f,
        minimumMajorTickSpacing = 30.dp,
        minorTickCount = 4,
    )

    var pointer by remember { mutableStateOf<Offset?>(null) }
    var bounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) pointer = awaitPointerEvent().changes.first().position } }
            .onGloballyPositioned { bounds = it }
    ) {
        XYGraph(
            xAxisModel = xModel,
            yAxisModel = yModel,
            gestureConfig = GestureConfig(
                panXEnabled = true,
                panYEnabled = true,
                zoomXEnabled = true,
                zoomYEnabled = true,
                independentZoomEnabled = false
            ),
//            xAxisLabels = { Text("${"%.2f".format(it)}", fontSize = 10.sp) },
//            yAxisLabels = { Text("${"%.2f".format(it)}", fontSize = 10.sp) }
        ) {
            datasets.forEachIndexed { di, cd ->
                cd.series.forEachIndexed { si, series ->
                    val pts = series.map { Point(it.first.toFloat(), it.second.toFloat()) }
                    val visible = visibilityStates[di][si]
                    val baseColor = colors[si % colors.size]
                    // Choose style per file
                    val style = when (di) {
                        0 -> LineStyle(brush = SolidColor(baseColor), strokeWidth = if (visible) 2.dp else 1.dp)
                        1 -> LineStyle(brush = SolidColor(baseColor), strokeWidth = 2.dp,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f,10f)))
                        2 -> LineStyle(brush = SolidColor(baseColor), strokeWidth = 2.dp,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f,10f)))
                        else -> LineStyle(brush = SolidColor(baseColor), strokeWidth = 2.dp)
                    }
                    LinePlot(
                        pts,
                        lineStyle = style,
                        symbol = if (di == 2) { point ->
                            Box(Modifier.size(4.dp)
                                .background(baseColor.copy(alpha = if (visible) 1f else 0.3f), CircleShape))
                        } else null
                    )
                }
            }
//            // Hover tooltip
//            pointer?.let { px -> bounds?.let { b ->
//                val loc = b.windowToLocal(px)
//                val w = b.size.width.toFloat(); val h = b.size.height.toFloat()
//                if (loc.x in 0f..w && loc.y in 0f..h) {
//                    val nx = (loc.x / w).coerceIn(0f,1f)
//                    val ny = (1f - loc.y / h).coerceIn(0f,1f)
//                    val dx = xModel.range.start + nx * (xModel.range.endInclusive - xModel.range.start)
//                    val dy = yModel.range.start + ny * (yModel.range.endInclusive - yModel.range.start)
//                    Box(
//                        Modifier.offset { IntOffset(px.x.toInt()+8, px.y.toInt()-24) }
//                            .background(Color(0xDD333333), RoundedCornerShape(4.dp))
//                            .padding(4.dp)
//                    ) { Text("x=${"%.2f".format(dx)}, y=${"%.2f".format(dy)}", fontSize = 12.sp, color=Color.White) }
//                }
//            }}
        }
    }
}
