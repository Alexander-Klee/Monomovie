package de.amklee.monomovie

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.serialization.json.Json
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
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

@OptIn(DelicateCoroutinesApi::class)
inline fun <reified T> HttpClient.subscribeSse(urlString: String, crossinline act: suspend (T) -> Unit): Job = GlobalScope.launch {
    sse(urlString) {
        incoming.collect {
            if (it.data == "heartbeat") return@collect
            act(Json.decodeFromString<T>(it.data!!))
        }
    }
}
