package de.amklee.monomovie.dto

import kotlinx.serialization.Serializable

@Serializable
data class OfferItemUI(val name: String?, val webUrl: String?, val iconUrl: String?) {
    constructor(offer: Offer) : this(
        name = offer.`package`?.clearName?.ifBlank { null },
        webUrl = offer.standardWebURL?.ifBlank { null },
        iconUrl = offer.`package`?.icon?.ifBlank { null }?.let { "https://images.justwatch.com$it" },
    )

    companion object {
        fun fromJellyfinLink(jellyfinLink: String?, jellyfinLogoLink: String?): OfferItemUI? {
            if (jellyfinLink == null) return null
            return OfferItemUI(
                name = "Jellyfin",
                webUrl = jellyfinLink,
                iconUrl = jellyfinLogoLink,
            )
        }
    }
}
