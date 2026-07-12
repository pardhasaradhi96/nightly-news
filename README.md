# Nightly News — your personal news app

A small local app that pulls fresh headlines from free RSS feeds (BBC, TechCrunch,
The Hindu, ESPN, NASA, etc. — no API key needed), shown in a clean dark-mode
reading view so you can catch up before bed.

## 1. Setup (one time)

You need Python 3.9+ installed. Then in a terminal:

```bash
cd news-app
pip install -r requirements.txt
```

(If `pywebview` fails to install and you don't care about the desktop-window
option below, you can skip it — just remove that line from requirements.txt
or run `pip install flask feedparser` instead.)

## 2. Run it in your browser

```bash
python app.py
```

Then open: **http://127.0.0.1:5000**

You'll see category tabs (Top Stories, India, Technology, Business, Sports,
Science), a dark/light toggle, and a refresh button. News is cached for 15
minutes per category so it doesn't refetch every click, and the page also
auto-refreshes itself every 15 minutes.

## 3. Turn it into an actual "app" (3 options, pick one)

**Option A — Installable web app (easiest, 30 seconds)**
With `app.py` running, open `http://127.0.0.1:5000` in Chrome/Edge, click the
install icon in the address bar (or menu → "Install Nightly News"). It now
opens in its own window with its own icon in your taskbar/dock, just like a
native app.

**Option B — Native desktop window (no browser UI at all)**
```bash
pip install pywebview
python desktop_app.py
```
This opens the same app inside a plain window with no address bar or tabs —
feels like a standalone desktop app. Closing the window quits it.

**Option C — Package as a standalone .exe/.app (most "appy")**
Once Option B works, you can freeze it with PyInstaller:
```bash
pip install pyinstaller
pyinstaller --onefile --add-data "templates:templates" --add-data "static:static" desktop_app.py
```
This produces a single executable in `dist/` you can double-click — no
terminal, no "python" command needed.

## 4. Customize your sources

Open `app.py` and edit the `FEEDS` dictionary near the top — add, remove, or
swap any RSS feed URL per category. Most major news sites publish an RSS feed
(search "site name + RSS feed").

## Notes

- Some RSS feeds occasionally go offline or rename their URL — if a source
  stops showing items, just check the feed URL still works in a browser.
- Everything runs locally on your machine; nothing is sent to any third party
  except the RSS requests themselves (same as visiting those news sites).
