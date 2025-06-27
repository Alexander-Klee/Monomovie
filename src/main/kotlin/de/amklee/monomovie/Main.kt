package de.amklee.monomovie

val bannedTypes = setOf("BUY", "RENT")

suspend fun main() {
    val justWatch = JustWatch(country = "DE", language = "en")

    val searchResult = justWatch.search("12 angry men")


    val bestFitMovie = searchResult[0]
    println("Best fit movie: ${bestFitMovie.content?.title}")
    val offers = bestFitMovie.offers?.filter { it.monetizationType !in bannedTypes } ?: emptyList()

    println(justWatch.details(bestFitMovie.id!!) == bestFitMovie)

    for (offer in offers) {
        println("Found offer: ${offer.`package`?.clearName} at ${offer.standardWebURL}, presentationType: ${offer.presentationType}, monetizationType: ${offer.monetizationType}")
    }

}