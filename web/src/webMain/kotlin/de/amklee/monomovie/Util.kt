package de.amklee.monomovie

import kotlinx.browser.document
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import org.w3c.dom.Document
import org.w3c.dom.Element
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

expect val Document.create: TagConsumer<Element>
expect val HTMLElement.append: TagConsumer<HTMLElement>
expect val HTMLElement.prepend: TagConsumer<HTMLElement>

fun Document.create(build: FlowContent.() -> Unit) = this.create
    .apply {
        DIV(mapOf(), this).build()
    }.finalize()

fun HTMLElement.append(build: FlowContent.() -> Unit) = this.append
    .apply {
        DIV(mapOf(), this).build()
    }.finalize()

fun HTMLElement.prepend(build: FlowContent.() -> Unit) = this.prepend
    .apply {
        DIV(mapOf(), this).build()
    }.finalize()
