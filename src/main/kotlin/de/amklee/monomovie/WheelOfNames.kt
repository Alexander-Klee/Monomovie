package de.amklee.monomovie

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WheelOfNames(private val apiKey: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
        defaultRequest {
            url("https://wheelofnames.com/api/v2/wheels")
            contentType(ContentType.Application.Json)
        }
    }

    @Serializable
    data class Entry(val text: String, val weight: Int)
    @Serializable
    private data class Wheel(
        val shareMode: String,
        val wheelConfig: WheelConfig
    ) {
        @Serializable
        data class WheelConfig(
            val title: String,
            val description: String,
            val entries: List<Entry>
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
                title = "My wheel",
                description = "I created this wheel using the API!",
                entries = entries
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
