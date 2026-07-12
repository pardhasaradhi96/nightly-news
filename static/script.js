const container = document.getElementById("newsContainer");
const tabs = document.querySelectorAll(".tab");
const refreshBtn = document.getElementById("refreshBtn");
const themeBtn = document.getElementById("themeBtn");
const fetchedAtEl = document.getElementById("fetchedAt");
const stateSelector = document.getElementById("stateSelector");
const stateDropdown = document.getElementById("stateDropdown");

let currentCategory = document.querySelector(".tab.active").dataset.category;

function timeAgo(dateStr) {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  if (isNaN(d)) return dateStr;
  return d.toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

function renderNews(items) {
  if (!items || items.length === 0) {
    container.innerHTML = `<p class="empty">No headlines found right now. Try refreshing.</p>`;
    return;
  }
  container.innerHTML = items.map(item => `
    <a class="card" href="${item.link}" target="_blank" rel="noopener noreferrer">
      <span class="source">${item.source}</span>
      <h3>${item.title}</h3>
      ${item.summary ? `<p>${item.summary}</p>` : ""}
      <time>${timeAgo(item.published)}</time>
    </a>
  `).join("");
}

async function loadNews(category, forceRefresh = false) {
  container.innerHTML = `<p class="loading">Loading headlines...</p>`;
  const endpoint = forceRefresh ? "/api/refresh" : "/api/news";
  try {
    const res = await fetch(`${endpoint}?category=${encodeURIComponent(category)}`);
    const data = await res.json();
    renderNews(data.items);
    fetchedAtEl.textContent = `Updated ${data.fetched_at}`;
  } catch (err) {
    container.innerHTML = `<p class="empty">Could not load news. Check your internet connection.</p>`;
    console.error(err);
  }
}

async function loadStateNews(state, forceRefresh = false) {
  container.innerHTML = `<p class="loading">Loading headlines...</p>`;
  const endpoint = forceRefresh ? "/api/state-refresh" : "/api/state-news";
  try {
    const res = await fetch(`${endpoint}?state=${encodeURIComponent(state)}`);
    const data = await res.json();
    renderNews(data.items);
    fetchedAtEl.textContent = `Updated ${data.fetched_at}`;
  } catch (err) {
    container.innerHTML = `<p class="empty">Could not load news. Check your internet connection.</p>`;
    console.error(err);
  }
}

tabs.forEach(tab => {
  tab.addEventListener("click", () => {
    tabs.forEach(t => t.classList.remove("active"));
    tab.classList.add("active");

    if (tab.dataset.category === "__state__") {
      stateSelector.style.display = "block";
      if (stateDropdown.value) {
        loadStateNews(stateDropdown.value);
      } else {
        container.innerHTML = `<p class="empty">Pick a state or union territory above.</p>`;
        fetchedAtEl.textContent = "";
      }
    } else {
      stateSelector.style.display = "none";
      currentCategory = tab.dataset.category;
      loadNews(currentCategory);
    }
  });
});

stateDropdown.addEventListener("change", () => {
  if (stateDropdown.value) loadStateNews(stateDropdown.value);
});

function isStateTabActive() {
  const activeTab = document.querySelector(".tab.active");
  return activeTab && activeTab.dataset.category === "__state__";
}

refreshBtn.addEventListener("click", () => {
  if (isStateTabActive() && stateDropdown.value) {
    loadStateNews(stateDropdown.value, true);
  } else if (!isStateTabActive()) {
    loadNews(currentCategory, true);
  }
});

themeBtn.addEventListener("click", () => {
  const html = document.documentElement;
  const isLight = html.getAttribute("data-theme") === "light";
  html.setAttribute("data-theme", isLight ? "dark" : "light");
  themeBtn.textContent = isLight ? "☾" : "☀";
  localStorage.setItem("newsapp-theme", isLight ? "dark" : "light");
});

// Restore saved theme
const savedTheme = localStorage.getItem("newsapp-theme");
if (savedTheme === "light") {
  document.documentElement.setAttribute("data-theme", "light");
  themeBtn.textContent = "☀";
}

// Initial load + auto-refresh every 15 minutes
loadNews(currentCategory);
setInterval(() => {
  if (isStateTabActive()) {
    if (stateDropdown.value) loadStateNews(stateDropdown.value);
  } else {
    loadNews(currentCategory);
  }
}, 15 * 60 * 1000);
