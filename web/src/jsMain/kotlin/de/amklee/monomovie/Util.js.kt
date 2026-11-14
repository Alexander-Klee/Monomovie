package de.amklee.monomovie

import kotlinx.html.TagConsumer
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.dom.prepend
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

actual val Document.create: TagConsumer<Element> get() = create
actual val HTMLElement.append: TagConsumer<HTMLElement> get() = append
actual val HTMLElement.prepend: TagConsumer<HTMLElement> get() = prepend
