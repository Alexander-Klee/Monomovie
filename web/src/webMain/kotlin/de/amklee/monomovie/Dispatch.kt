package de.amklee.monomovie

import de.amklee.monomovie.dto.JsCall
import kotlinx.browser.document

fun JsCall.dispatch() {
    when (this) {
        is JsCall.SetupSse -> document.addEventListener("DOMContentLoaded") {
            Sse.setup(mode)
        }
    }
}
