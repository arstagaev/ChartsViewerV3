package ru.atrsx.chartviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import java.awt.FileDialog

@Composable
@Preview
fun App() {
    var data1 by remember { mutableStateOf<ChartData?>(null) }
    var data2 by remember { mutableStateOf<ChartData?>(null) }
    var visibility1 by remember { mutableStateOf(List(8) { true }) }
    var visibility2 by remember { mutableStateOf(List(8) { true }) }

    // Define colors for buttons (same as chart lines)
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                FileDialog(null as java.awt.Frame?, "Select File 1", FileDialog.LOAD).apply {
                    isVisible = true
                    file?.let { fn ->
                        parseChartFile("$directory$fn")?.let { data ->
                            data1 = data
                            visibility1 = data.visibility
                        }
                    }
                }
            }) {
                Text("Load File 1")
            }

            Button(onClick = {
                FileDialog(null as java.awt.Frame?, "Select File 2", FileDialog.LOAD).apply {
                    isVisible = true
                    file?.let { fn ->
                        parseChartFile("$directory$fn")?.let { data ->
                            data2 = data
                            visibility2 = data.visibility
                        }
                    }
                }
            }) {
                Text("Load File 2")
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val datasets = listOfNotNull(data1, data2)
            val visibilityStates = listOf(visibility1, visibility2).take(datasets.size)
            if (datasets.isNotEmpty()) {
                ChartView(datasets, visibilityStates, "Combined Datasets")
            }
        }

        // Visibility toggle buttons for each series
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (seriesIndex in 0 until 8) {
                val isVisibleFile1 = data1?.let { visibility1.getOrNull(seriesIndex) ?: false } ?: false

                val isSeriesAvailable = (data1?.series?.getOrNull(seriesIndex)?.isNotEmpty() == true) ||
                        (data2?.series?.getOrNull(seriesIndex)?.isNotEmpty() == true)

                if (isSeriesAvailable) {
                    Box(modifier = Modifier.background(seriesColors[seriesIndex % seriesColors.size]).clickable {
                            visibility1 = visibility1.toMutableList().apply {
                                if (seriesIndex < size) set(seriesIndex, !get(seriesIndex))
                            }
                        }
                    ) {
                        Text(text = "Series $seriesIndex: ${if (isVisibleFile1) "On" else "Off"}", fontSize = 15.sp, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (seriesIndex in 0 until 8) {
                val isVisibleFile2 = data2?.let { visibility2.getOrNull(seriesIndex) ?: false } ?: false
                val isSeriesAvailable = (data1?.series?.getOrNull(seriesIndex)?.isNotEmpty() == true) ||
                        (data2?.series?.getOrNull(seriesIndex)?.isNotEmpty() == true)

                if (isSeriesAvailable) {
                    Box(modifier = Modifier.background(seriesColors[seriesIndex % seriesColors.size]).clickable {
                        visibility2 = visibility2.toMutableList().apply {
                            if (seriesIndex < size) set(seriesIndex, !get(seriesIndex))
                        }
                    }
                    ) {
                        Text(text = "Series $seriesIndex: ${if (isVisibleFile2) "On" else "Off"}",fontSize = 15.sp, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}