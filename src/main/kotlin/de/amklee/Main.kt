package de.amklee

val bannedTypes = setOf("BUY", "RENT")

suspend fun main() {
    val justWatch = JustWatch(country = "DE", language = "en")

    val searchResult = justWatch.search("12 angry men")

//    for (entry in searchResult) {
////        println(entry)
//
//    }

    val bestFitMovie = searchResult[0]
    println("Best fit movie: ${bestFitMovie.content?.title}")
    val offers = bestFitMovie.offers?.filter { it.monetizationType !in bannedTypes } ?: emptyList()

    for (offer in offers) {
        println("Found offer: ${offer.`package`?.clearName} at ${offer.standardWebURL}, presentationType: ${offer.presentationType}, monetizationType: ${offer.monetizationType}")
    }

}