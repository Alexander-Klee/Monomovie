package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.Offer
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.p

fun FlowContent.OfferPage(movie: CachedMovies.Movie, offers: Map<String, List<Offer>>) {
    val title = movie.mediaEntry.content?.title
    title?.let { h1 { +"Offers for $title" } }

    for ((country, offers) in offers) {
        h1 { +country }
        if (offers.isEmpty()) {
            p { +"No offers found" }
        } else {
            for (offer in offers) {
                p {
                    +(offer.`package`?.clearName + " - " + offer.monetizationType + " - " + (offer.retailPrice?.let { "$it ${offer.currency}" } ?: "Free"))
                }
                a {
                    href = offer.standardWebURL ?: ""
                    +(offer.standardWebURL ?: "No link")
                }
            }
        }
    }
}