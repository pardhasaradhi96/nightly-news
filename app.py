"""
Personal Night-Time News App
-----------------------------
A simple local web app that pulls headlines from free public RSS feeds
(no API key required) and shows them in a clean, dark-mode reading view.

Run it with:  python app.py
Then open:    http://127.0.0.1:5000
"""
import os
from flask import Flask, render_template, jsonify, request
import feedparser
import re
import threading
from datetime import datetime, timedelta

app = Flask(__name__)

# ---------------------------------------------------------------------
# News sources, grouped by category. Feel free to add/remove/edit these.
# ---------------------------------------------------------------------
FEEDS = {
    "Top Stories": [
        ("BBC News", "http://feeds.bbci.co.uk/news/rss.xml"),
        ("Reuters", "https://www.reutersagency.com/feed/?best-topics=top-news"),
        ("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml"),
    ],
    "India": [
        ("BBC Asia", "https://feeds.bbci.co.uk/news/world/asia/india/rss.xml"),
        ("The Hindu", "https://www.thehindu.com/news/national/feeder/default.rss"),
        ("Times of India", "https://timesofindia.indiatimes.com/rssfeedstopstories.cms"),
    ],
    "Technology": [
        ("TechCrunch", "https://techcrunch.com/feed/"),
        ("The Verge", "https://www.theverge.com/rss/index.xml"),
        ("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index"),
    ],
    "Business": [
        ("BBC Business", "http://feeds.bbci.co.uk/news/business/rss.xml"),
        ("CNBC", "https://www.cnbc.com/id/100003114/device/rss/rss.html"),
    ],
    "Sports": [
        ("BBC Sport", "http://feeds.bbci.co.uk/sport/rss.xml"),
        ("ESPN", "https://www.espn.com/espn/rss/news"),
    ],
    "Science": [
        ("BBC Science", "http://feeds.bbci.co.uk/news/science_and_environment/rss.xml"),
        ("NASA", "https://www.nasa.gov/rss/dyn/breaking_news.rss"),
    ],
    "Cinema": [
        ("BBC Entertainment", "http://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml"),
        ("Times of India Entertainment", "https://timesofindia.indiatimes.com/rssfeeds/1081479906.cms"),
        ("Variety", "https://variety.com/feed/"),
        ("The Hollywood Reporter", "https://www.hollywoodreporter.com/feed/"),
    ],
}

# ---------------------------------------------------------------------
# Indian States & Union Territories — used for the "State News" dropdown.
# There's no fixed RSS feed per state, so we build a live Google News
# search query for whichever state/UT the user picks.
# ---------------------------------------------------------------------
STATES = [
    "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
    "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
    "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya",
    "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim",
    "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand",
    "West Bengal",
    # Union Territories
    "Andaman and Nicobar Islands", "Chandigarh",
    "Dadra and Nagar Haveli and Daman and Diu", "Delhi",
    "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry",
]

# In-memory cache so we don't hammer the RSS servers on every refresh
_cache = {}
_cache_lock = threading.Lock()
CACHE_DURATION = timedelta(minutes=15)


def clean_summary(text, max_len=220):
    """Strip HTML tags and trim length."""
    text = re.sub("<[^<]+?>", "", text or "")
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) > max_len:
        text = text[:max_len].rsplit(" ", 1)[0] + "..."
    return text


def parse_date(entry):
    """Return a sortable datetime, falling back to 'now' if missing."""
    for key in ("published_parsed", "updated_parsed"):
        val = entry.get(key)
        if val:
            try:
                return datetime(*val[:6])
            except Exception:
                pass
    return datetime.min


def fetch_feed(source_name, url, is_google_news=False):
    items = []
    try:
        parsed = feedparser.parse(url)
        for entry in parsed.entries[:12]:
            title = entry.get("title", "Untitled")
            entry_source = source_name
            summary = ""

            if is_google_news:
                # Google News RSS has no real article summary — its
                # "description" is just the headline + publisher name
                # glued together. Pull the actual publisher out instead
                # of showing a duplicated headline.
                src = entry.get("source")
                if isinstance(src, dict) and src.get("title"):
                    entry_source = src["title"]
            else:
                raw_summary = entry.get("summary", entry.get("description", ""))
                summary = clean_summary(raw_summary)
                # Safety net: if a feed's summary just repeats the title, drop it.
                if summary.lower().startswith(title.lower()):
                    summary = summary[len(title):].strip(" -–—|\u00a0")

            items.append({
                "title": title,
                "link": entry.get("link", "#"),
                "summary": summary,
                "source": entry_source,
                "published": entry.get("published", ""),
                "_sort": parse_date(entry).isoformat(),
            })
    except Exception as e:
        print(f"[warn] Could not fetch {source_name}: {e}")
    return items


def get_cached(key, fetcher):
    """Generic cache lookup. `fetcher` is a zero-arg function returning items."""
    now = datetime.now()
    with _cache_lock:
        cached = _cache.get(key)
        if cached and now - cached["time"] < CACHE_DURATION:
            return cached["items"]

    all_items = fetcher()
    all_items.sort(key=lambda x: x["_sort"], reverse=True)

    with _cache_lock:
        _cache[key] = {"time": now, "items": all_items}
    return all_items


def get_news_for_category(category):
    def fetcher():
        items = []
        for source_name, url in FEEDS.get(category, []):
            items.extend(fetch_feed(source_name, url))
        return items
    return get_cached(f"cat:{category}", fetcher)


def fetch_state_news(state):
    """Build a live Google News RSS search for the given state/UT."""
    import urllib.parse
    query = urllib.parse.quote(f"{state} news")
    url = f"https://news.google.com/rss/search?q={query}&hl=en-IN&gl=IN&ceid=IN:en"
    return fetch_feed(f"Google News — {state}", url, is_google_news=True)


def get_news_for_state(state):
    return get_cached(f"state:{state}", lambda: fetch_state_news(state))


@app.route("/")
def index():
    return render_template("index.html", categories=list(FEEDS.keys()), states=STATES)


@app.route("/api/news")
def api_news():
    category = request.args.get("category", "Top Stories")
    if category not in FEEDS:
        category = "Top Stories"
    items = get_news_for_category(category)
    return jsonify({
        "category": category,
        "items": items,
        "fetched_at": datetime.now().strftime("%I:%M %p"),
    })


@app.route("/api/refresh")
def api_refresh():
    """Force-refresh a category, bypassing the cache."""
    category = request.args.get("category", "Top Stories")
    with _cache_lock:
        _cache.pop(f"cat:{category}", None)
    items = get_news_for_category(category)
    return jsonify({
        "category": category,
        "items": items,
        "fetched_at": datetime.now().strftime("%I:%M %p"),
    })


@app.route("/api/states")
def api_states():
    return jsonify({"states": STATES})


@app.route("/api/state-news")
def api_state_news():
    state = request.args.get("state", "")
    if state not in STATES:
        return jsonify({"error": "Unknown state/UT"}), 400
    items = get_news_for_state(state)
    return jsonify({
        "state": state,
        "items": items,
        "fetched_at": datetime.now().strftime("%I:%M %p"),
    })


@app.route("/api/state-refresh")
def api_state_refresh():
    state = request.args.get("state", "")
    if state not in STATES:
        return jsonify({"error": "Unknown state/UT"}), 400
    with _cache_lock:
        _cache.pop(f"state:{state}", None)
    items = get_news_for_state(state)
    return jsonify({
        "state": state,
        "items": items,
        "fetched_at": datetime.now().strftime("%I:%M %p"),
    })


if __name__ == "__main__":
    print("\nStarting your news app...")
    print("Open this in your browser:  http://127.0.0.1:5000\n")
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
