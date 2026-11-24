package de.amklee.monomovie.util

import de.amklee.monomovie.Environment
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.withCharset
import io.ktor.resources.serialization.ResourcesFormat
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.plugin
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.resources.Resources
import io.ktor.server.resources.resource
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.sse.ServerSSESessionWithSerialization
import io.ktor.server.sse.sse
import kotlinx.html.HTML
import kotlinx.html.consumers.delayed
import kotlinx.html.html
import kotlinx.html.stream.HTMLStreamBuilder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

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

context(resourcesFormat: ResourcesFormat)
inline fun <reified T> hrefFull(resource: T): String {
    val urlBuilder = URLBuilder(Environment.hostname)
    href(resource, urlBuilder)
    return urlBuilder.buildString()
}

// This mess is from the ktor-server-resource plugin.
// Apparently, mixing it with SSE is not intended :(
inline fun <reified T : Any> Route.sse(crossinline handler: suspend ServerSSESessionWithSerialization.(T) -> Unit): Route {
    lateinit var builtRoute: Route
    resource<T> {
        installResourceInstancePlugin(serializer<T>())
        builtRoute = sse(
            serialize = { typeInfo, it ->
                val serializer = Json.serializersModule.serializer(typeInfo.kotlinType!!)
                Json.encodeToString(serializer, it)
            },
            handler = {
                @Suppress("UNCHECKED_CAST")
                val resource = call.attributes[Internal.getResourceInstanceKey()] as T
                handler(resource)
            },
        )
    }
    return builtRoute
}

fun <T> Route.installResourceInstancePlugin(serializer: KSerializer<T>) {
    install(ResourceInstancePlugin) {
        this.serializer = serializer
    }
}

private class ResourceInstancePluginConfig {
    lateinit var serializer: KSerializer<*>
}

private val ResourceInstancePlugin = createRouteScopedPlugin("ResourceInstancePlugin", ::ResourceInstancePluginConfig) {
    val serializer = pluginConfig.serializer
    onCall { call ->
        val resources = call.application.plugin(Resources)
        try {
            val resource = resources.resourcesFormat.decodeFromParameters(serializer, call.parameters) as Any
            call.attributes.put(Internal.getResourceInstanceKey(), resource)
        } catch (cause: Throwable) {
            throw BadRequestException("Can't transform call to resource", cause)
        }
    }
}
