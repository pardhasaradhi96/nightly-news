package com.nightlynews.app

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object NewsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 NightlyNews/1.0")
                .build()
            chain.proceed(req)
        }
        .build()

    private val parser = RssParser()

    val categories = listOf(
        "Top Stories", "India", "Technology", "Business", "Sports", "Science", "Cinema"
    )

    private val feeds = mapOf(
        "Top Stories" to listOf(
            "BBC News"    to "http://feeds.bbci.co.uk/news/rss.xml",
            "Al Jazeera"  to "https://www.aljazeera.com/xml/rss/all.xml"
        ),
        "India" to listOf(
            "BBC India"        to "https://feeds.bbci.co.uk/news/world/asia/india/rss.xml",
            "Times of India"   to "https://timesofindia.indiatimes.com/rssfeedstopstories.cms",
            "The Hindu"        to "https://www.thehindu.com/news/national/feeder/default.rss"
        ),
        "Technology" to listOf(
            "TechCrunch"  to "https://techcrunch.com/feed/",
            "The Verge"   to "https://www.theverge.com/rss/index.xml",
            "Ars Technica" to "https://feeds.arstechnica.com/arstechnica/index"
        ),
        "Business" to listOf(
            "BBC Business" to "http://feeds.bbci.co.uk/news/business/rss.xml",
            "CNBC"         to "https://www.cnbc.com/id/100003114/device/rss/rss.html"
        ),
        "Sports" to listOf(
            "BBC Sport" to "http://feeds.bbci.co.uk/sport/rss.xml",
            "ESPN"      to "https://www.espn.com/espn/rss/news"
        ),
        "Science" to listOf(
            "BBC Science" to "http://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
            "NASA"        to "https://www.nasa.gov/rss/dyn/breaking_news.rss"
        ),
        "Cinema" to listOf(
            "BBC Entertainment" to "http://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml",
            "TOI Entertainment" to "https://timesofindia.indiatimes.com/rssfeeds/1081479906.cms",
            "Variety"           to "https://variety.com/feed/",
            "Hollywood Reporter" to "https://www.hollywoodreporter.com/feed/"
        )
    )

    val states = listOf(
        "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
        "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
        "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya",
        "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim",
        "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand",
        "West Bengal",
        // Union Territories
        "Andaman and Nicobar Islands", "Chandigarh",
        "Dadra and Nagar Haveli and Daman and Diu", "Delhi",
        "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
    )

    fun fetchCategory(category: String): List<NewsItem> {
        val feedList = feeds[category] ?: return emptyList()
        val all = mutableListOf<NewsItem>()
        for ((source, url) in feedList) {
            try { all.addAll(fetchFeed(url, source)) } catch (_: Exception) {}
        }
        return all.sortedByDescending { it.rawDate }
    }

    fun fetchState(state: String): List<NewsItem> {
        val query = URLEncoder.encode("$state news", "UTF-8")
        val url = "https://news.google.com/rss/search?q=$query&hl=en-IN&gl=IN&ceid=IN:en"
        return try {
            fetchFeed(url, "Google News", isGoogleNews = true)
        } catch (_: Exception) { emptyList() }
    }

    private fun fetchFeed(
        url: String,
        sourceName: String,
        isGoogleNews: Boolean = false
    ): List<NewsItem> {
        val req = Request.Builder().url(url).build()
        val resp = client.newCall(req).execute()
        return resp.use { r ->
            if (!r.isSuccessful) return@use emptyList()
            val body = r.body ?: return@use emptyList()
            parser.parse(body.byteStream(), sourceName, isGoogleNews)
        }
    }
}
