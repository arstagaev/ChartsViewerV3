package ru.atrsx.chartviewer

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ChartViewer V3",
        state = rememberWindowState(width = 1000.dp, height = 800.dp)
    ) {
        App()
    }
}
