package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.Offer
import de.amklee.monomovie.components.Mode
import de.amklee.monomovie.components.MovieItem
import de.amklee.monomovie.components.OfferList
import de.amklee.monomovie.util.Resources
import kotlinx.html.*
import java.util.Locale

enum class MonetizationTypes(val type: String) {
    FLATRATE("Flatrate"),
    RENT("Rent"),
    BUY("Buy"),
    FREE("Free")
}

private fun FlowContent.offerTable(offers: List<Offer>) {
    table {
        for (type in MonetizationTypes.entries) {
            tr {
                val typeOffers = offers.filter { it.monetizationType.equals(type.name, ignoreCase = true) }
                if (typeOffers.isNotEmpty()) {
                    td(classes = "offerType-td") { +type.type }
                    td(classes = "offer-td") {
                        OfferList(typeOffers.sortedBy { it.`package`?.clearName })
                    }
                }
            }
        }
    }
}

private fun getCountryName(countryCode: String): String {
    return try {
        val locale = Locale.Builder()
            .setRegion(countryCode)
            .build()
        locale.getDisplayCountry(Locale.ENGLISH)
    } catch (e: Exception) {
        countryCode
    }
}

suspend fun FlowContent.OfferPage(movie: CachedMovies.Movie, offers: Map<String, List<Offer>>, mainCountry: String = "DE") {
    div {
        script {
            unsafe {
                +Resources.bookmarkJs
                +Resources.watchedJs
                +Resources.sseJs(Mode.OFFERS)
            }
        }
        MovieItem(movie, showOffers = false)

        // sort countries by number of flatrate offers descending, prioritize main country
        val sortedOffers = offers.toList()
            .sortedByDescending {
                (_, offers) -> offers.filter {
                    it.monetizationType.equals("Flatrate", ignoreCase = true)
                }.size }
            .sortedBy {
                (country, _) -> if (country == mainCountry) 0 else 1
            }

        // countries with offers
        for ((country, offers) in sortedOffers.filter { it.second.isNotEmpty() }) {
            h1 { +country
                 +" - "
                 +getCountryName(country) }
            offerTable(offers)
        }

        val countriesWithoutOffers = offers.filter { it.value.isEmpty() }.keys.sorted()
        if (countriesWithoutOffers.isNotEmpty()) {
            h1 { +"No offers for:"}
            p { +countriesWithoutOffers.joinToString(", ") }
        }
    }
}