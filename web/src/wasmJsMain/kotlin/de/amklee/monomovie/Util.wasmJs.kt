package de.amklee.monomovie

import kotlinx.html.Entities
import kotlinx.html.ExperimentalKotlinxHtmlApi
import kotlinx.html.Tag
import kotlinx.html.TagConsumer
import kotlinx.html.Unsafe
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.dom.prepend
import kotlinx.html.org.w3c.dom.events.Event
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

actual val Document.create: TagConsumer<Element> get() = create
actual val HTMLElement.append: TagConsumer<HTMLElement> get() = Shim(append)
actual val HTMLElement.prepend: TagConsumer<HTMLElement> get() = Shim(prepend)

private class Shim(private val consumer: TagConsumer<Element>) : TagConsumer<HTMLElement> {
    @ExperimentalKotlinxHtmlApi override val head: Any? get() = consumer.head
    override fun onTagStart(tag: Tag) = consumer.onTagStart(tag)
    override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) = consumer.onTagAttributeChange(tag, attribute, value)
    override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) = consumer.onTagEvent(tag, event, value)
    override fun onTagEnd(tag: Tag) = consumer.onTagEnd(tag)
    override fun onTagContent(content: CharSequence) = consumer.onTagContent(content)
    override fun onTagContentEntity(entity: Entities) = consumer.onTagContentEntity(entity)
    override fun onTagContentUnsafe(block: Unsafe.() -> Unit) = consumer.onTagContentUnsafe(block)
    override fun onTagComment(content: CharSequence) = consumer.onTagComment(content)
    override fun finalize(): HTMLElement = consumer.finalize() as HTMLElement
}
