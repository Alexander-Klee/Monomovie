package de.amklee.monomovie.components

import de.amklee.monomovie.dto.JsCall
import kotlinx.html.BUTTON
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.ExperimentalKotlinxHtmlApi
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.impl.dataset
import kotlinx.html.onClick

@Suppress("FunctionName")
@OptIn(ExperimentalKotlinxHtmlApi::class)
inline fun FlowContent.CustomToggleButton(
    classes: String = "",
    initialState: Boolean,
    checkedBody: DIV.() -> Unit,
    checkAction: JsCall,
    uncheckedBody: DIV.() -> Unit,
    uncheckAction: JsCall,
    block: BUTTON.() -> Unit = {},
) {
    button(
        type = ButtonType.button,
        classes = "$classes checked-scope",
    ) {
        onClick = """
            if (dataset.checked == 'true') {
                dataset.checked = 'false';
                ${checkAction.toJsString()}
            } else {
                dataset.checked = 'true';
                ${uncheckAction.toJsString()}
            }
        """.trimIndent()

        dataset["checked"] = initialState.toString()

        block()

        div(classes = "in-checked") {
            checkedBody()
        }

        div(classes = "in-unchecked") {
            uncheckedBody()
        }
    }
}
