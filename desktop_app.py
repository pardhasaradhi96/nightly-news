"""
Optional: run this INSTEAD of app.py to get the news app in its own
native desktop window (no browser tab/address bar), like a real app.

Setup:
    pip install pywebview

Run:
    python desktop_app.py
"""

import threading
import webview
from app import app


def start_flask():
    app.run(port=5000, debug=False, use_reloader=False)


if __name__ == "__main__":
    t = threading.Thread(target=start_flask, daemon=True)
    t.start()
    webview.create_window("Nightly News", "http://127.0.0.1:5000", width=1100, height=750)
    webview.start()
