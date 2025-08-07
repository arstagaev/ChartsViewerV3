package ru.atrsx.chartviewer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.atrsx.chartviewer.koala.xygraph.XYGraph
import ru.atrsx.chartviewer.koala.xygraph.rememberFloatLinearAxisModel
import ru.atrsx.chartviewer.koala.style.LineStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import ru.atrsx.chartviewer.koala.xygraph.Point
import ru.atrsx.chartviewer.koala.ExperimentalKoalaPlotApi
import ru.atrsx.chartviewer.koala.gestures.GestureConfig
import ru.atrsx.chartviewer.koala.line.LinePlot
import ru.atrsx.chartviewer.koala.xygraph.autoScaleXRange
import ru.atrsx.chartviewer.koala.xygraph.autoScaleYRange
import java.io.File

/**
 * Represents a chart dataset with visibility flags and data series.
 */
data class ChartData(
    val series: List<List<Pair<Double, Double>>>,
    val visibility: List<Boolean>
)

/**
 * Parses a file with the specified format:
 * - #standard#<filename>
 * - #visibility#<0 or 1 for each series>
 * - Data lines: x;y|x;y|... for up to 8 series
 */
fun parseChartFile(path: String): ChartData? {
    return try {
        val lines = File(path).bufferedReader().useLines { it.toList() }
        if (lines.size < 2) return null

        // Parse visibility line (e.g., #visibility#1#1#1#1#1#1#1#1)
        val visibility = lines[1].split("#")
            .drop(2)
            .mapNotNull { it.toIntOrNull() }
            .map { it == 1 }
            .take(8)

        // Parse data lines
        val dataLines = lines.dropWhile { it.startsWith("#") }
        val series = (0 until visibility.size).map { mutableListOf<Pair<Double, Double>>() }

        dataLines.forEach { line ->
            val pairs = line.split("|").mapNotNull { segment ->
                segment.split(";").takeIf { it.size == 2 }
                    ?.let { (x, y) ->
                        x.toDoubleOrNull()?.let { xVal ->
                            y.toDoubleOrNull()?.let { yVal -> xVal to yVal }
                        }
                    }
            }
            pairs.forEachIndexed { index, pair ->
                if (index < series.size) series[index].add(pair)
            }
        }

        ChartData(series, visibility)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private val defaultMaxPoints get() = 800
private val defaultMinPoints get() = 400

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun ChartView(
    datasets: List<ChartData>,
    visibilityStates: List<List<Boolean>>,
    title: String
) {

    // Collect all visible series based on dynamic visibility
    val allVisibleSeries = datasets.flatMapIndexed { datasetIndex, chartData ->
        chartData.series.filterIndexed { seriesIndex, _ ->
            seriesIndex < visibilityStates[datasetIndex].size && visibilityStates[datasetIndex][seriesIndex]
        }
    }

    // Convert to points for plotting
    val allPoints = allVisibleSeries.flatMap { series ->
        series.map { (x, y) -> Point(x.toFloat(), y.toFloat()) }
    }

    // Auto-scale axes based on all visible points
    val xModel = rememberFloatLinearAxisModel(allPoints.takeIf { it.isNotEmpty() }?.autoScaleXRange() ?: 0f..1f)
    val yModel = rememberFloatLinearAxisModel(allPoints.takeIf { it.isNotEmpty() }?.autoScaleYRange() ?: 0f..1f)

    // Define colors for each series index (up to 8 series)
    val seriesColors = listOf(
        Color.Blue,
        Color.Red,
        Color.Green,
        Color.Magenta,
        Color.Cyan,
        Color.Yellow,
        Color.Gray,
        Color.Black
    )

    XYGraph(xModel, yModel,
        gestureConfig = GestureConfig(
            panXEnabled = true,
            panYEnabled = true,
            zoomXEnabled = true,
            zoomYEnabled = true,
            independentZoomEnabled = false
        ),
        ) {
        // Iterate over each series index (0 to 7)
        for (seriesIndex in 0 until 8) {
            val seriesList = datasets.mapNotNull { chartData ->
                if (seriesIndex < chartData.series.size &&
                    seriesIndex < visibilityStates[datasets.indexOf(chartData)].size &&
                    visibilityStates[datasets.indexOf(chartData)][seriesIndex]
                ) {
                    chartData.series[seriesIndex].map { (x, y) -> Point(x.toFloat(), y.toFloat()) }
                } else {
                    null
                }
            }

            // Plot each series with the same color but different styles based on dataset
            seriesList.forEachIndexed { datasetIndex, series ->
                val color = seriesColors[seriesIndex % seriesColors.size]
                val lineStyle = when (datasetIndex) {
                    0 -> LineStyle(SolidColor(color)) // Solid line for file 1
                    1 -> LineStyle(
                        SolidColor(color),
                        strokeWidth = 2.dp,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
                    ) // Dashed line for file 2
                    else -> LineStyle(SolidColor(Color.Gray)) // Fallback
                }
                LinePlot(series, lineStyle = lineStyle,
//                    symbol = { point ->
//                        Symbol(
//                            shape = CircleShape,
//                            fillBrush = SolidColor(Color.Black),
//                            modifier = Modifier.then(
//                                Modifier.hoverableElement {
//                                    HoverSurface { Text(point.y.toString()) }
//                                }
//                            )
//                        )
//                    }
                )

            }
        }
    }
}

@Composable
fun HoverSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = Color.LightGray,
        modifier = modifier.padding(2.dp)
    ) {
        Box(modifier = Modifier.padding(2.dp)) {
            content()
        }
    }
}

/**
 * Extension function to zoom an axis around a pivot point.
 */
//fun FloatLinearAxisModel.zoom(zoomFactor: Float, pivot: Float): ClosedFloatingPointRange<Float> {
//    val currentRange = range
//    val width = currentRange.endInclusive - currentRange.start
//    val pivotValue = currentRange.start + width * pivot
//    val newWidth = width * zoomFactor
//    val newStart = pivotValue - newWidth * pivot
//    val newEnd = pivotValue + newWidth * (1 - pivot)
//    return newStart..newEnd
//}