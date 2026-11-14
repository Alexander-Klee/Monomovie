package de.amklee.monomovie

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTemplateElement
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.get

inline fun NodeList.forEach(action: (node: Node) -> Unit) {
    for (i in 0 until this.length) {
        this[i]?.let(action)
    }
}

inline fun NodeList.forEachElement(action: (element: HTMLElement) -> Unit) {
    forEach {
        if (it is HTMLElement) action(it)
    }
}

fun parseHtmlElement(html: String): Node? {
    val template = document.createElement("template") as HTMLTemplateElement
    template.innerHTML = html.trim()
    if (template.content.childElementCount != 1) {
        println("Expected exactly one root element in HTML, but found ${template.content.childElementCount}.")
        return null
    }
    return template.content.firstChild
}
