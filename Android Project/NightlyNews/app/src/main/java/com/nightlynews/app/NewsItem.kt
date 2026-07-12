package com.nightlynews.app

data class NewsItem(
    val title: String,
    val link: String,
    val summary: String,
    val source: String,
    val published: String,
    val rawDate: Long = 0L
)
