package de.amklee.monomovie.components

import kotlinx.html.ExperimentalKotlinxHtmlApi
import kotlinx.html.FlowContent
import kotlinx.html.impl.dataset
import kotlinx.html.submitInput

@Suppress("FunctionName")
@OptIn(ExperimentalKotlinxHtmlApi::class)
fun FlowContent.RouletteSelectionButton(label: String = "Roulette", minSelection: Int = 2) {
    submitInput(classes = "button require-min-selection") {
        disabled = true
        dataset["min-selection"] = minSelection.toString()
        value = label
    }
}
