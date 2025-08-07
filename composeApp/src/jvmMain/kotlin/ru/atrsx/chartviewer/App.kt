package ru.atrsx.chartviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

// --- Data model holds Float Points directly ---
data class ChartData(
    val series: List<List<Point<Float, Float>>>,
    val visibility: List<Boolean>
)

// --- Suspend parser runs on IO dispatcher ---
suspend fun parseChartFile(path: String): ChartData? = withContext(Dispatchers.IO) {
    try {
        val lines = File(path).bufferedReader().useLines { it.toList() }
        if (lines.size < 2) return@withContext null

        val visibility = lines[1]
            .split("#")
            .drop(2)
            .mapNotNull { it.toIntOrNull() }
            .map { it == 1 }
            .take(8)

        val dataLines = lines.dropWhile { it.startsWith("#") }
        val floatSeries = List(8) { mutableListOf<Point<Float, Float>>() }
        dataLines.forEach { line ->
            line.split("|").mapNotNull { seg ->
                seg.split(";").takeIf { it.size == 2 }?.let { (xs, ys) ->
                    xs.toDoubleOrNull()?.toFloat()?.let { x ->
                        ys.toDoubleOrNull()?.toFloat()?.let { y ->
                            Point(x, y)
                        }
                    }
                }
            }.forEachIndexed { i, p -> if (i < floatSeries.size) floatSeries[i].add(p) }
        }
        ChartData(floatSeries, visibility)
    } catch (e: Exception) {
        e.printStackTrace(); null
    }
}

@Composable
@Preview
fun App() {
    // file paths
    var path1 by remember { mutableStateOf<String?>(null) }
    var path2 by remember { mutableStateOf<String?>(null) }
    var path3 by remember { mutableStateOf<String?>(null) }

    // load data asynchronously
    val data1 by produceState<ChartData?>(initialValue = null, path1) { value = path1?.let { parseChartFile(it) } }
    val data2 by produceState<ChartData?>(initialValue = null, path2) { value = path2?.let { parseChartFile(it) } }
    val data3 by produceState<ChartData?>(initialValue = null, path3) { value = path3?.let { parseChartFile(it) } }

    // individual series visibility
    var vis1 by remember { mutableStateOf(List(8) { true }) }
    var vis2 by remember { mutableStateOf(List(8) { true }) }
    var vis3 by remember { mutableStateOf(List(8) { true }) }
    // file-level visibility
    val fileVisible1 = vis1.any { it }
    val fileVisible2 = vis2.any { it }
    val fileVisible3 = vis3.any { it }

    // reset per-file visibility when file changes
    LaunchedEffect(data1) { data1?.let { vis1 = it.visibility } }
    LaunchedEffect(data2) { data2?.let { vis2 = it.visibility } }
    LaunchedEffect(data3) { data3?.let { vis3 = it.visibility } }

    val seriesColors = listOf(
        Color.Blue, Color.Red, Color.Green, Color.Magenta,
        Color.Cyan, Color.Yellow, Color.Gray, Color.Black
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // File selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                FileButton("File 1", path1, listOf(path1, path2, path3)) { path1 = it }
                FileButton("File 2", path2, listOf(path1, path2, path3)) { path2 = it }
                FileButton("File 3", path3, listOf(path1, path2, path3)) { path3 = it }
            }
            Column {
                // Per-series toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (path1 != null) FileControl(
                        name = File(path1!!).name,
                        color = Color.Blue,
                        visible = fileVisible1,
                        onToggle = { vis1 = vis1.map { !fileVisible1 } },
                        onClear = { path1 = null }
                    )
                    ToggleSeriesButtons(vis1, data1?.series, seriesColors) { vis1 = it }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (path2 != null) FileControl(
                        name = File(path2!!).name,
                        color = Color.Red,
                        visible = fileVisible2,
                        onToggle = { vis2 = vis2.map { !fileVisible2 } },
                        onClear = { path2 = null }
                    )
                    ToggleSeriesButtons(vis2, data2?.series, seriesColors) { vis2 = it }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (path3 != null) FileControl(
                        name = File(path3!!).name,
                        color = Color.Green,
                        visible = fileVisible3,
                        onToggle = { vis3 = vis3.map { !fileVisible3 } },
                        onClear = { path3 = null }
                    )
                    ToggleSeriesButtons(vis3, data3?.series, seriesColors) { vis3 = it }
                }
            }
        }

        // File name & clear/toggle row
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//
//
//
//        }

        // Chart area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val datasets = listOfNotNull(data1, data2, data3)
            val visibilityStates = listOf(vis1, vis2, vis3)
            if (datasets.isNotEmpty()) {
                ChartView(
                    datasets = datasets,
                    visibilityStates = visibilityStates,
                    colors = seriesColors
                )
            }
        }


    }
}

@Composable
private fun FileControl(
    name: String,
    color: Color,
    visible: Boolean,
    onToggle: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(name, fontSize = 12.sp)
        Button(onClick = onToggle) { Text(if (visible) "Hide" else "Show", fontSize = 12.sp) }
        Button(onClick = onClear)  { Text("Clear", fontSize = 12.sp) }
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
                val dir = directory
                val full = if (dir.endsWith(File.separator)) "$dir$fn" else "$dir${File.separator}$fn"
                if (!existingPaths.contains(full)) onFileSelected(full)
            }
        }
    }) { Text(label) }
}

@Composable
fun ToggleSeriesButtons(
    visibility: List<Boolean>,
    seriesList: List<List<Point<Float, Float>>>?,
    colors: List<Color>,
    onChange: (List<Boolean>) -> Unit
) {
    seriesList?.forEachIndexed { idx, series ->
        if (series.isNotEmpty()) {
            val bg = if (visibility[idx]) colors[idx] else colors[idx].copy(alpha = 0.3f)
            Box(
                modifier = Modifier
                    .background(bg, RoundedCornerShape(4.dp))
                    .clickable { onChange(visibility.toMutableList().apply { this[idx] = !this[idx] }) }
                    .padding(8.dp)
            ) {
                Text("Ch$idx", fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun ChartView(
    datasets: List<ChartData>,
    visibilityStates: List<List<Boolean>>,
    colors: List<Color>
) {
    val maxPoints = 6000
    val minPoints = 200

    // Per-file line style effects (dashes)
    val fileEffects = listOf<PathEffect?>(
        null,
        PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        PathEffect.dashPathEffect(floatArrayOf(2f, 6f))
    )

    val downsampledAll by remember(datasets, visibilityStates) {
        derivedStateOf {
            datasets.flatMapIndexed { di, cd ->
                cd.series.flatMapIndexed { si, series ->
                    if (!visibilityStates[di][si]) emptyList()
                    else {
                        val step = (series.size / minPoints).coerceAtLeast(1)
                        if (series.size > maxPoints)
                            series.filterIndexed { idx, _ -> idx % step == 0 }
                        else series
                    }
                }
            }
        }
    }

    val xRange = downsampledAll
        .takeIf { it.isNotEmpty() }
        ?.autoScaleXRange()
        ?: (0f..1f)
    val yRange = downsampledAll
        .takeIf { it.isNotEmpty() }
        ?.autoScaleYRange()
        ?: (0f..1f)

    val xModel = rememberFloatLinearAxisModel(
        range = xRange,
        minViewExtent = (xRange.endInclusive - xRange.start) * 0.01f,
        maxViewExtent = xRange.endInclusive - xRange.start,
        minimumMajorTickIncrement = (xRange.endInclusive - xRange.start) * 0.005f,
        minimumMajorTickSpacing = 30.dp,
        minorTickCount = 4
    )
    val yModel = rememberFloatLinearAxisModel(
        range = yRange,
        minViewExtent = (yRange.endInclusive - yRange.start) * 0.01f,
        maxViewExtent = yRange.endInclusive - yRange.start,
        minimumMajorTickIncrement = (yRange.endInclusive - yRange.start) * 0.005f,
        minimumMajorTickSpacing = 30.dp,
        minorTickCount = 4
    )

    Box(Modifier.fillMaxSize()) {
        XYGraph(
            xAxisModel = xModel,
            yAxisModel = yModel,
            modifier = Modifier.fillMaxSize(),
            gestureConfig = GestureConfig(
                panXEnabled = true,
                panYEnabled = true,
                zoomXEnabled = true,
                zoomYEnabled = true,
                independentZoomEnabled = false
            )
        ) {
            datasets.forEachIndexed { di, cd ->
                cd.series.forEachIndexed { si, series ->
                    if (!visibilityStates[di][si]) return@forEachIndexed
                    val step = (series.size / minPoints).coerceAtLeast(1)
                    val plotData = if (series.size > maxPoints)
                        series.filterIndexed { idx, _ -> idx % step == 0 }
                    else series

                    // Color per series index
                    val baseColor = colors[si % colors.size]
                    // Style dashing per file index
                    val effect = fileEffects.getOrNull(di)
                    val style = LineStyle(
                        brush = SolidColor(baseColor),
                        strokeWidth = 2.dp,
                        pathEffect = effect
                    )

                    LinePlot(
                        data = plotData,
                        lineStyle = style,
                        symbol = if (di == 2) { point ->
                            Box(
                                Modifier
                                    .size(4.dp)
                                    .background(baseColor, CircleShape)
                            )
                        } else null
                    )
                }
            }
        }
    }
}
