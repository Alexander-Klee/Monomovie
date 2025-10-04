package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.Offer
import de.amklee.monomovie.components.MovieItem
import de.amklee.monomovie.components.OfferList
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.tr

private val monetizationTypes = setOf("Flatrate", "Rent", "Buy", "Free")

private fun FlowContent.offerTable(offers: List<Offer>) {
    table {
        for (type in monetizationTypes) {
            tr {
                val typeOffers = offers.filter { it.monetizationType.equals(type, ignoreCase = true) }
                if (typeOffers.isNotEmpty()) {
                    td(classes = "offerType-td") { +type }
                    td(classes = "offer-td") {
                        OfferList(typeOffers)
                    }
                }
            }
        }
    }
}

fun FlowContent.OfferPage(movie: CachedMovies.Movie, offers: Map<String, List<Offer>>) {
    div {
        MovieItem(movie, showOffers = false)

        for ((country, offers) in offers) {
            h1 { +country }
            if (offers.isEmpty()) {
                p { +"No offers found" }
            } else {
                offerTable(offers)
            }
        }
    }
}