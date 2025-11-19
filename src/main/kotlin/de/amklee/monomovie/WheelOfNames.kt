package de.amklee.monomovie

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WheelOfNames(private val apiKey: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        defaultRequest {
            url("https://wheelofnames.com/api/v2/wheels")
            contentType(ContentType.Application.Json)
        }
    }

    @Serializable
    data class Entry(val text: String, val weight: Int) {
        init {
            require(text.isNotBlank()) { "Entry text must not be blank" }
            require(weight >= 0) { "Entry weight must be non-negative" }
        }
    }
    @Serializable
    private data class Wheel(
        val shareMode: String,
        val wheelConfig: WheelConfig
    ) {
        @Serializable
        data class WheelConfig(
            val title: String,
            val description: String,
            val entries: List<Entry>,
            val isAdvanced: Boolean
        )
    }
    @Serializable
    private data class Response(
        val data: Data
    ) {
        @Serializable
        data class Data(
            val path: String
        )
    }

    suspend fun createWheel(entries: List<Entry>): String {
        val wheel = Wheel(
            shareMode = "copyable",
            wheelConfig = Wheel.WheelConfig(
                title = "Vie Randomover",
                description = "I created this cyberwheel using the API!",
                entries = entries,
                isAdvanced = true
            )
        )
        try {
            return "https://wheelofnames.com/" + client.post {
                headers.apply {
                    set("Content-Type", "application/json")
                    set("x-api-key", apiKey)
                }
                setBody(wheel)
            }.body<Response>().data.path
        } catch (error: Exception) {
            throw Exception("Could not generate wheel", error)
        }
    }
}
