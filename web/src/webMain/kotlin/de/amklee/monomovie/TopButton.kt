package de.amklee.monomovie

import de.amklee.monomovie.components.ArrowUpSvg
import kotlinx.browser.document
import kotlinx.html.button
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import org.w3c.dom.SMOOTH
import org.w3c.dom.ScrollBehavior
import org.w3c.dom.ScrollToOptions
import org.w3c.dom.set

object TopButton {
    fun inject() {
        val scrollTopButton = document.body!!.append {
            button(classes = "floating-action-button top-button") {
                id = "top_button"
                onClickFunction = {
                    document.getElementById("main")
                        ?.scrollTo(ScrollToOptions(top = 0.0, behavior = ScrollBehavior.SMOOTH))
                }
                ArrowUpSvg(classes = "top-button-icon")
            }
        }
        console.log("Injected top button", scrollTopButton)
        document.addEventListener("scroll") {
            val shown = document.getElementById("main")?.scrollTop?.let { it > 1000 } ?: false
            scrollTopButton.dataset["shown"] = shown.toString()
        }
    }
}
