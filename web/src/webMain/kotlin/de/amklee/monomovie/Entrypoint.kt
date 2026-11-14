package de.amklee.monomovie

import de.amklee.monomovie.dto.SseEvent
import kotlinx.browser.document

@OptIn(ExperimentalJsExport::class)
@JsExport
class Entrypoint {
    fun setupSse(mode: String) {
        document.addEventListener("DOMContentLoaded") {
            Sse.setup(SseEvent.Mode.valueOf(mode))
        }
    }
}
