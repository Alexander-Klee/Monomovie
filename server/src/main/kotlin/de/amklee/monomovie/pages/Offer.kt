package de.amklee.monomovie.pages

import de.amklee.monomovie.R
import de.amklee.monomovie.components.MovieItem
import de.amklee.monomovie.components.OfferItem
import de.amklee.monomovie.components.OfferList
import de.amklee.monomovie.components.invoke
import de.amklee.monomovie.dto.*
import de.amklee.monomovie.dto.JsCall.SetupSse
import java.util.*
import kotlinx.html.*

private fun FlowContent.offerTable(offers: List<Offer>) {
    table {
        for (type in MonetizationTypes.entries) {
            tr {
                val typeOffers = offers.filter {
                    it.monetizationType.equals(type.name, ignoreCase = true)
                }
                if (typeOffers.isNotEmpty()) {
                    td(classes = "offerType-td") { +type.type }
                    td(classes = "offer-td") {
                        OfferList(
                            typeOffers
                                .sortedBy { it.`package`?.clearName }
                                .map(::OfferItemUI),
                        )
                    }
                }
            }
        }
    }
}

private fun getCountryName(countryCode: String): String = try {
    val locale =
        Locale
            .Builder()
            .setRegion(countryCode)
            .build()
    locale.getDisplayCountry(Locale.ENGLISH)
} catch (e: Exception) {
    countryCode
}

@HtmlTagMarker
context(jellyfinItems: JellyfinItems)
private fun FlowContent.JellyfinTable(movie: Movie) {
    val jellyfinLink = jellyfinItems[movie] ?: return
    h1 { +"Jellyfin" }
    table {
        tr {
            td(classes = "offerType-td") { +"Jellyfin" }
            td(classes = "offer-td") {
                ul(classes = "offer-list") {
                    OfferItem(OfferItemUI.fromJellyfinLink(jellyfinLink, jellyfinItems.logoLink)!!)
                }
            }
        }
    }
}

@HtmlTagMarker
context(jellyfinItems: JellyfinItems)
fun FlowContent.OfferPage(movie: Movie, offers: Map<String, List<Offer>>, mainCountry: String = "DE") {
    div {
        script {
            type = "module"
            unsafe {
                +R.imageErrorJs
                +SetupSse(SseEvent.Mode.OFFERS).toJsString()
            }
        }
        MovieItem(MovieItemUI(movie), showOffers = false)

        JellyfinTable(movie)

        // sort countries by number of flatrate offers descending, prioritize main country
        val sortedOffers =
            offers
                .toList()
                .sortedByDescending { (_, offers) ->
                    offers
                        .filter {
                            it.monetizationType.equals("Flatrate", ignoreCase = true)
                        }.size
                }.sortedBy { (country, _) ->
                    if (country == mainCountry) 0 else 1
                }

        // countries with offers
        for ((country, offers) in sortedOffers.filter { it.second.isNotEmpty() }) {
            h1 {
                +country
                +" - "
                +getCountryName(country)
            }
            offerTable(offers)
        }

        val countriesWithoutOffers = offers.filter { it.value.isEmpty() }.keys.sorted()
        if (countriesWithoutOffers.isNotEmpty()) {
            h1 { +"No offers for:" }
            p { +countriesWithoutOffers.joinToString(", ") }
        }
    }
}
