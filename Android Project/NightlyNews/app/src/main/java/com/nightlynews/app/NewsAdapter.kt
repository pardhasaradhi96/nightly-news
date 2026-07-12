package com.nightlynews.app

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nightlynews.app.databinding.ItemNewsBinding

class NewsAdapter : ListAdapter<NewsItem, NewsAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<NewsItem>() {
            override fun areItemsTheSame(a: NewsItem, b: NewsItem) = a.link == b.link
            override fun areContentsTheSame(a: NewsItem, b: NewsItem) = a == b
        }
    }

    inner class ViewHolder(private val b: ItemNewsBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: NewsItem) {
            b.tvSource.text    = item.source.uppercase()
            b.tvTitle.text     = item.title
            b.tvPublished.text = item.published

            if (item.summary.isNotEmpty()) {
                b.tvSummary.text = item.summary
                b.tvSummary.visibility = View.VISIBLE
            } else {
                b.tvSummary.visibility = View.GONE
            }

            b.root.setOnClickListener {
                try {
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()

                    customTabsIntent.launchUrl(
                        it.context,
                        Uri.parse(item.link)
                    )
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
