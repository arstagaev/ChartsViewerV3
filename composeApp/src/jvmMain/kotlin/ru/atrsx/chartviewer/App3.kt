package ru.atrsx.chartviewer

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.io.File
import javax.swing.JFileChooser

@Composable
@Preview
fun App3() {
    var file1 by remember { mutableStateOf<String?>(null) }
    var file2 by remember { mutableStateOf<String?>(null) }
    var file3 by remember { mutableStateOf<String?>(null) }
    var chart by remember { mutableStateOf<JFreeChart?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        // File 1 Selection
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                val fileChooser = JFileChooser()
                val result = fileChooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    file1 = fileChooser.selectedFile.absolutePath
                }
            }) {
                Text("Select File 1")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(file1 ?: "No file selected")
        }

        // File 2 Selection
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                val fileChooser = JFileChooser()
                val result = fileChooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    file2 = fileChooser.selectedFile.absolutePath
                }
            }) {
                Text("Select File 2")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(file2 ?: "No file selected")
        }

        // File 3 Selection
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                val fileChooser = JFileChooser()
                val result = fileChooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    file3 = fileChooser.selectedFile.absolutePath
                }
            }) {
                Text("Select File 3")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(file3 ?: "No file selected")
        }

        // Plot Button
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (file1 != null && file2 != null && file3 != null) {
                val series1 = parseFirstSeries(File(file1!!))
                val series2 = parseFirstSeries(File(file2!!))
                val series3 = parseFirstSeries(File(file3!!))

                val xySeries1 = XYSeries("File 1")
                series1.forEach { xySeries1.add(it.first, it.second) }
                val xySeries2 = XYSeries("File 2")
                series2.forEach { xySeries2.add(it.first, it.second) }
                val xySeries3 = XYSeries("File 3")
                series3.forEach { xySeries3.add(it.first, it.second) }

                val dataset = XYSeriesCollection().apply {
                    addSeries(xySeries1)
                    addSeries(xySeries2)
                    addSeries(xySeries3)
                }

                val newChart = ChartFactory.createXYLineChart(
                    "Chart from Three Files", // Chart title
                    "Time",                   // X-axis label
                    "Value",                  // Y-axis label
                    dataset                   // Data
                )
                chart = newChart
            }
        }) {
            Text("Plot")
        }

        // Chart Display
        if (chart != null) {
            SwingPanel(
                factory = { ChartPanel(chart) },
                modifier = Modifier.fillMaxSize().padding(top = 16.dp)
            )
        }
    }
}

/**
 * Parses the first series from a file.
 * Each data line is split by '|', and the first 'time;value' pair is extracted.
 */
fun parseFirstSeries(file: File): List<Pair<Double, Double>> {
    val series = mutableListOf<Pair<Double, Double>>()
    file.bufferedReader().use { reader ->
        var line: String?
        // Skip headers until blank line
        while (reader.readLine().also { line = it } != null) {
            if (line!!.isBlank()) break
        }
        // Parse data lines
        while (reader.readLine().also { line = it } != null) {
            if (line!!.isNotBlank()) {
                val parts = line!!.split("|")
                if (parts.isNotEmpty()) {
                    val firstPair = parts[0].split(";")
                    if (firstPair.size == 2) {
                        val time = firstPair[0].toDouble()
                        val value = firstPair[1].toDouble()
                        series.add(Pair(time, value))
                    }
                }
            }
        }
    }
    return series
}