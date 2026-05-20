package me.vladislav.quotes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.vladislav.orders.OrderSide
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

class QuotesClient(
    private val baseUrl: String,
    private val timeout: Duration = Duration.ofSeconds(3),
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(timeout).build()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun marketPrice(instrumentId: Long, side: OrderSide, authorization: String?): BigDecimal =
        withContext(Dispatchers.IO) {
            val response = send(
                path = "/quotes/market-price?instrumentId=$instrumentId&side=${side.name}",
                authorization = authorization
            )
            if (response.statusCode() >= 300) {
                throw QuoteUnavailableException("Price unavailable for instrument $instrumentId")
            }
            kopecksToRub(json.decodeFromString<MarketPriceDto>(response.body()).priceKopecks)
        }

    suspend fun currentPrices(instrumentIds: Collection<Long>, authorization: String?): Map<Long, BigDecimal> {
        val ids = instrumentIds.distinct()
        if (ids.isEmpty()) return emptyMap()

        return withContext(Dispatchers.IO) {
            val encodedIds = urlEncode(ids.joinToString(","))
            val response = send(
                path = "/quotes/current?instrumentIds=$encodedIds",
                authorization = authorization
            )
            if (response.statusCode() >= 300) return@withContext emptyMap()
            json.decodeFromString<List<QuoteDto>>(response.body())
                .associate { it.instrumentId to kopecksToRub(it.lastPriceKopecks ?: it.last ?: 0L) }
                .filterValues { it > BigDecimal.ZERO }
        }
    }

    private fun send(path: String, authorization: String?): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl.trimEnd('/')}$path"))
            .timeout(timeout)
            .header("Accept", "application/json")
            .apply {
                if (!authorization.isNullOrBlank()) header("Authorization", authorization)
            }
            .GET()
            .build()

        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun kopecksToRub(kopecks: Long): BigDecimal =
        BigDecimal(kopecks).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}

class QuoteUnavailableException(message: String) : RuntimeException(message)

@Serializable
private data class MarketPriceDto(
    val priceKopecks: Long
)

@Serializable
private data class QuoteDto(
    val instrumentId: Long,
    val lastPriceKopecks: Long? = null,
    val last: Long? = null
)
