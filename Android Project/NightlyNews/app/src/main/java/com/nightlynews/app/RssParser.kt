package com.nightlynews.app

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class RssParser {

    fun parse(
        inputStream: InputStream,
        defaultSource: String,
        isGoogleNews: Boolean = false
    ): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)

        try {
            parser.setInput(inputStream, null)
        } catch (e: Exception) {
            return emptyList()
        }

        var inItem = false
        var title = ""
        var link = ""
        var description = ""
        var pubDate = ""
        var articleSource = defaultSource
        var currentTag = ""
        var count = 0

        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT && count < 25) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name ?: ""
                        when (currentTag) {
                            "item", "entry" -> {
                                inItem = true
                                title = ""; link = ""; description = ""
                                pubDate = ""; articleSource = defaultSource
                            }
                            "link" -> if (inItem) {
                                // Atom feeds store href in attribute
                                val href = parser.getAttributeValue(null, "href")
                                if (!href.isNullOrEmpty() && link.isEmpty()) link = href
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (inItem) {
                            val text = parser.text?.trim() ?: ""
                            if (text.isEmpty()) { eventType = parser.next(); continue }
                            when (currentTag) {
                                "title"               -> if (title.isEmpty())       title = text
                                "link"                -> if (link.isEmpty())        link = text
                                "description",
                                "summary",
                                "content"             -> if (description.isEmpty()) description = text
                                "pubDate",
                                "published",
                                "dc:date",
                                "updated"             -> if (pubDate.isEmpty())     pubDate = text
                                "source"              -> if (isGoogleNews)          articleSource = text
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val tag = parser.name ?: ""
                        if ((tag == "item" || tag == "entry") && inItem) {
                            inItem = false
                            if (title.isNotEmpty()) {
                                val cleanTitle = title.stripHtml()
                                val rawMs = pubDate.toEpochMs()

                                val summary = if (isGoogleNews) {
                                    "" // Google News description is just the headline again
                                } else {
                                    val s = description.stripHtml().truncate(230)
                                    // Drop summary if it just repeats the title
                                    if (s.lowercase().take(40) == cleanTitle.lowercase().take(40)) "" else s
                                }

                                items.add(
                                    NewsItem(
                                        title = cleanTitle,
                                        link = link,
                                        summary = summary,
                                        source = articleSource,
                                        published = pubDate.formatDisplay(),
                                        rawDate = rawMs
                                    )
                                )
                                count++
                            }
                            currentTag = ""
                        } else if (tag == currentTag) {
                            currentTag = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) { /* partial parse is fine */ }

        return items
    }

    // ---- helpers ----

    private fun String.stripHtml(): String =
        this.replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&nbsp;", " ").replace("&#39;", "'")
            .replace(Regex("\\s+"), " ").trim()

    private fun String.truncate(max: Int): String =
        if (length > max) substring(0, max).substringBeforeLast(' ') + "…" else this

    private val PARSE_FORMATS = listOf(
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss"
    )

    private fun String.toEpochMs(): Long {
        if (isEmpty()) return 0L
        for (fmt in PARSE_FORMATS) {
            try {
                return SimpleDateFormat(fmt, Locale.ENGLISH).parse(this)?.time ?: continue
            } catch (_: Exception) {}
        }
        return 0L
    }

    private fun String.formatDisplay(): String {
        val ms = toEpochMs()
        if (ms == 0L) return this
        return SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).format(Date(ms))
    }
}
