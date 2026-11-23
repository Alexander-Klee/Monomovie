package de.amklee.monomovie.browser

import kotlinx.js.JsPlainObject
import org.w3c.dom.DOMRectReadOnly
import org.w3c.dom.Element

@JsPlainObject
external interface IntersectionObserverInit {
    var root: Element?
    var rootMargin: String
    var scrollMargin: String
    var threshold: Double
}

@JsPlainObject
external interface IntersectionObserverEntry {
    var boundingClientRect: DOMRectReadOnly
    var intersectionRatio: Double
    var intersectionRect: DOMRectReadOnly
    var isIntersecting: Boolean
    var rootBounds: DOMRectReadOnly?
    var target: Element
    var time: Double
}

external class IntersectionObserver(
    callback: (Array<IntersectionObserverEntry>, IntersectionObserver) -> Unit,
    options: IntersectionObserverInit = definedExternally,
) {
    fun observe(target: Element)
    fun unobserve(target: Element)
    fun disconnect()
}
