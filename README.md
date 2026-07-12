# 🌙 Nightly News

The main idea is to stay updated on everyday news in various categories.

[![Website](https://img.shields.io/badge/Website-Open-green)](https://nightly-news.onrender.com/)
[![APK](https://img.shields.io/badge/APK-Download-blue)](https://github.com/pardhasaradhi96/nightly-news/blob/main/Android%20Project/NightlyNews.apk)

A minimal, dark-mode news reader built for easy night-time reading — pulling live headlines from free public RSS feeds (no API keys needed). It ships as **two clients on one backend concept**: a Flask web app and a native Android app.

## ✨ Features

- **Category tabs** — Top Stories, India, Technology, Business, Sports, Science, Cinema
- **State News** — pick any Indian state/UT from a dropdown and get a live Google News search feed for it
- **Dark, distraction-free reading view** with a theme toggle (web)
- **Pull-to-refresh** (Android) / manual refresh button (web)
- **Server-side caching** (15-minute window) so repeated requests don't hammer the RSS sources
- **Clean summaries** — HTML stripped from descriptions, duplicate title/summary text automatically dropped
- Optional **native desktop window** mode via `pywebview` (no browser chrome)

## 🏗️ Architecture

The project has two independent front ends that both read from the same set of public RSS feeds:

| Client | Stack | Entry point |
|---|---|---|
| Web app | Python, Flask, `feedparser`, vanilla JS/CSS | `app.py` |
| Desktop wrapper | Flask app + `pywebview` native window | `desktop_app.py` |
| Android app | Kotlin, OkHttp, `XmlPullParser`-based RSS parser, MVVM (`ViewModel` + `LiveData`) | `Android Project/NightlyNews/` |

The Android app does **not** call the Flask backend — it fetches and parses the RSS feeds directly on-device (see `NewsRepository.kt` + `RssParser.kt`), so both clients work fully independently of each other.

### Web app request flow
```
Browser → Flask routes (/api/news, /api/state-news)
        → feedparser fetches & parses RSS
        → in-memory cache (15 min TTL)
        → JSON → rendered as cards by static/script.js
```

### Android app request flow
```
MainActivity → NewsViewModel → NewsRepository (OkHttp + coroutines)
             → RssParser (manual XML pull-parsing, supports RSS + Atom)
             → LiveData<UiState> → RecyclerView (NewsAdapter)
```

## 📂 Project Structure

```
nightly-news/
├── app.py                  # Flask app: routes, RSS fetching, caching
├── desktop_app.py          # Launches app.py inside a native pywebview window
├── requirements.txt        # flask, feedparser, pywebview, gunicorn
├── render.yaml             # Render.com deployment config
├── templates/
│   └── index.html          # Single-page shell (tabs, state dropdown, news grid)
├── static/
│   ├── style.css
│   └── script.js            # Fetches /api/* endpoints, renders cards, theme toggle
└── Android Project/
    ├── icon.png
    ├── NightlyNews.apk      # Prebuilt installable APK
    └── NightlyNews/         # Android Studio project (Kotlin, Gradle)
        └── app/src/main/java/com/nightlynews/app/
            ├── SplashActivity.kt
            ├── MainActivity.kt
            ├── NewsViewModel.kt      # UiState: Loading / Success / Error
            ├── NewsRepository.kt     # Feed URLs + OkHttp fetching
            ├── RssParser.kt          # Manual RSS/Atom XML parser
            ├── NewsAdapter.kt        # RecyclerView adapter, opens links in Custom Tabs
            └── NewsItem.kt           # Data model
```

## 🔌 API Endpoints (Flask backend)

| Endpoint | Description |
|---|---|
| `GET /` | Renders the main page |
| `GET /api/news?category=<name>` | Cached headlines for a category |
| `GET /api/refresh?category=<name>` | Bypasses cache, force-refetches a category |
| `GET /api/states` | List of Indian states/UTs |
| `GET /api/state-news?state=<name>` | Cached Google News results for a state |
| `GET /api/state-refresh?state=<name>` | Force-refresh state news |

## 🚀 Running Locally

### Web app
```bash
pip install -r requirements.txt
python app.py
# open http://127.0.0.1:5000
```

### Desktop (native window)
```bash
pip install pywebview
python desktop_app.py
```

### Android app
Open `Android Project/NightlyNews/` in Android Studio and run on an emulator/device (minSdk 26 / targetSdk 34), or just install the prebuilt `NightlyNews.apk` directly on a device.

## ☁️ Deployment

Deployed on **Render** using `render.yaml`:
- Build: `pip install -r requirements.txt`
- Start: `python app.py`
- Python version: 3.11.0

## 🛠️ Tech Stack

- **Backend:** Python, Flask, feedparser, gunicorn
- **Frontend (web):** HTML, CSS, vanilla JavaScript
- **Android:** Kotlin, OkHttp, AndroidX (ViewModel/LiveData, RecyclerView, Material Components), View Binding, Custom Tabs

## 📰 News Sources

Feeds are pulled from BBC, Al Jazeera, Reuters, The Hindu, Times of India, TechCrunch, The Verge, Ars Technica, CNBC, ESPN, NASA, Variety, and The Hollywood Reporter, plus a live Google News search for state-specific news. All sources are free public RSS feeds — no API keys required.

## 📌 Notes / Ideas for Improvement

- The web app and Android app currently duplicate the feed-source list — could be unified via a shared config, or the Android app could call the Flask `/api/*` endpoints instead of parsing RSS itself.
- No automated tests are currently included.
- `requirements.txt` includes `pywebview`, which isn't needed for the Render web deployment (only for the optional desktop wrapper) — could be split into a separate `requirements-desktop.txt`.

