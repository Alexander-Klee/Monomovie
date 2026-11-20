package de.amklee.monomovie.util

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondTextWriter
import kotlinx.html.HTML
import kotlinx.html.consumers.delayed
import kotlinx.html.html
import kotlinx.html.stream.HTMLStreamBuilder

suspend inline fun ApplicationCall.respondHtml(status: HttpStatusCode = HttpStatusCode.OK, crossinline block: suspend HTML.() -> Unit) {
    respondTextWriter(ContentType.Text.Html.withCharset(Charsets.UTF_8), status) {
        append("<!DOCTYPE html>\n")
        // delayed() delays the current tag head end until we have all attributes
        // this allows us to change attributes in the block, but shouldn't cause too much overhead
        HTMLStreamBuilder(this, prettyPrint = false, xhtmlCompatible = false).delayed().html {
            block()
        }
    }
}
