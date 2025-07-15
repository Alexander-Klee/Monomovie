package de.amklee.monomovie.util

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import kotlinx.html.stream.createHTML
import kotlin.contracts.ExperimentalContracts

// kotlinx.html does not come with helpers for easily building an HTML string, so we have to create our own.
// These are needed for the infinite scroll feature in the search page.
inline fun buildHtml(build: FlowContent.() -> Unit) = createHTML().apply {
    DIV(mapOf(), this).build()
}.finalize()
inline fun FlowContent.ful(content: UL.() -> Unit) = UL(mapOf(), consumer).content()
inline fun buildULHtml(build: UL.() -> Unit): String {
    return buildHtml {
        ful {
            build()
        }
    }
}

// Everything below this point exists solely because kotlinx.html decided adding crossinline in random places
// would be a good idea. It was not.
// (This is a workaround to be able to pass suspend functions as the HTML builder block)

/**
 * @see io.ktor.server.html.respondHtmlTemplate
 */
suspend inline fun <TTemplate : Template<HTML>> ApplicationCall.respondHtmlTemplate(
    template: TTemplate,
    status: HttpStatusCode = HttpStatusCode.OK,
    body: TTemplate.() -> Unit
) {
    template.body()
    respondHtml(status) { with(template) { apply() } }
}

/**
 * @see io.ktor.server.html.respondHtml
 */
suspend inline fun ApplicationCall.respondHtml(status: HttpStatusCode = HttpStatusCode.OK, block: HTML.() -> Unit) {
    val text = buildString {
        append("<!DOCTYPE html>\n")
        appendHTML().html {
            block()
        }
    }
    respond(TextContent(text, ContentType.Text.Html.withCharset(Charsets.UTF_8), status))
}

/**
 * @see kotlinx.html.html
 */
@HtmlTagMarker
@OptIn(ExperimentalContracts::class)
inline fun <T, C : TagConsumer<T>> C.html(namespace: String? = null, block: HTML.() -> Unit = {}) : T {
    return HTML(emptyMap, this, namespace)
        .visitAndFinalize(this, block)
}

/**
 * @see kotlinx.html.visitAndFinalize
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Tag, R> T.visitAndFinalize(
    consumer: TagConsumer<R>,
    block: T.() -> Unit
): R {
    return visitTagAndFinalize(consumer) {
        block()
    }
}
