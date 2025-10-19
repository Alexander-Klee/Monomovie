package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.Offer
import de.amklee.monomovie.components.MovieItem
import de.amklee.monomovie.components.OfferList
import kotlinx.html.*
import java.util.Locale

private val monetizationTypes = setOf("Flatrate", "Rent", "Buy", "Free")

private fun FlowContent.offerTable(offers: List<Offer>) {
    table {
        for (type in monetizationTypes) {
            tr {
                val typeOffers = offers.filter { it.monetizationType.equals(type, ignoreCase = true) }
                if (typeOffers.isNotEmpty()) {
                    td(classes = "offerType-td") { +type }
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

fun FlowContent.OfferPage(movie: CachedMovies.Movie, offers: Map<String, List<Offer>>, mainCountry: String = "DE") {
    div {
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