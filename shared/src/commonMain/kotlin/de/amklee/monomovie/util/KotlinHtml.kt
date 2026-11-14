package de.amklee.monomovie.util

import kotlinx.html.*
import kotlinx.html.stream.createHTML

// kotlinx.html does not come with helpers for easily building an HTML string, so we have to create our own.
// These are needed for the infinite scroll feature in the search page.
inline fun buildHtml(build: FlowContent.() -> Unit) = createHTML()
    .apply {
        DIV(mapOf(), this).build()
    }.finalize()

inline fun FlowContent.ful(content: UL.() -> Unit) = UL(mapOf(), consumer).content()

inline fun buildULHtml(build: UL.() -> Unit): String = buildHtml {
    ful {
        build()
    }
}

class CustomDomElement(tagName: String, namespace: String? = null, consumer: TagConsumer<*>) :
    HTMLTag(
        tagName,
        consumer,
        emptyMap(),
        namespace = namespace,
        inlineTag = true,
        emptyTag = false,
    ),
    HtmlInlineTag

fun FlowContent.custom(tagName: String, namespace: String? = null, block: CustomDomElement.() -> Unit = {}) {
    CustomDomElement(tagName, namespace, consumer).visit(block)
}
