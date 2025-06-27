package de.amklee


suspend fun main() {
    val justWatch = JustWatch(country = "DE", language = "en")

    val searchResult = justWatch.search("The Matrix")

    println(searchResult)

}