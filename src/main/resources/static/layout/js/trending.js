/**
 * Community Favourites Page — trending.js
 *
 * Fetches ALL ranking data once from /community/data, then filters client-side
 * when the user clicks a category tab — identical pattern to likedMedia.js.
 *
 * This avoids a full server round-trip (including external API calls to TMDB /
 * RAWG / Last.fm) every time a tab is clicked.
 */

let allTop10    = [];   // full unfiltered Top 10 list
let allTrending = [];   // full unfiltered Fast-Rising list
let currentFilter = "ALL";

// ─── Boot ─────────────────────────────────────────────────────────────────────

document.addEventListener("DOMContentLoaded", () => {
    loadCommunityData();

    document.querySelectorAll(".tab-btn").forEach(tab => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".tab-btn").forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            currentFilter = tab.dataset.type;
            renderFiltered();
        });
    });
});

// ─── Fetch ────────────────────────────────────────────────────────────────────

async function loadCommunityData() {
    setStatus("top10Status",    "Loading…");
    setStatus("trendingStatus", "Loading…");

    try {
        const res = await fetch("/community/data");

        if (!res.ok) {
            setStatus("top10Status",    "Could not load ranking data.");
            setStatus("trendingStatus", "Could not load trending data.");
            return;
        }

        const data = await res.json();
        allTop10    = data.top10    || [];
        allTrending = data.trending || [];

        setStatus("top10Status",    "");
        setStatus("trendingStatus", "");

        // Record VIEW interactions for every Top 10 item (same as before)
        allTop10.forEach(m => recordView(m.mediaApiId, m.mediaType));

        renderFiltered();
    } catch (err) {
        setStatus("top10Status",    "Could not load ranking data.");
        setStatus("trendingStatus", "Could not load trending data.");
        console.error("Community data fetch error:", err);
    }
}

// ─── Render ───────────────────────────────────────────────────────────────────

function renderFiltered() {
    const top10Items = currentFilter === "ALL"
        ? allTop10
        : allTop10.filter(m => m.mediaType === currentFilter);

    const trendingItems = currentFilter === "ALL"
        ? allTrending
        : allTrending.filter(m => m.mediaType === currentFilter);

    renderTop10(top10Items);
    renderTrending(allTrending);
}

function renderTop10(items) {
    const grid     = document.getElementById("top10Grid");
    const emptyMsg = document.getElementById("top10Empty");
    const statusEl = document.getElementById("top10Status");

    grid.innerHTML = "";

    if (items.length === 0) {
        if (emptyMsg) emptyMsg.style.display = "block";
        statusEl.textContent = allTop10.length === 0
            ? "No rankings yet — start interacting with media on the home page!"
            : "No ranked media in this category yet.";
        return;
    }

    if (emptyMsg) emptyMsg.style.display = "none";
    statusEl.textContent = "";

    items.forEach((m, idx) => grid.appendChild(buildTop10Card(m, idx + 1)));
}

function renderTrending(items) {
    const grid     = document.getElementById("trendingGrid");
    const emptyMsg = document.getElementById("trendingEmpty");
    const statusEl = document.getElementById("trendingStatus");

    grid.innerHTML = "";

    if (items.length === 0) {
        if (emptyMsg) emptyMsg.style.display = "block";
        statusEl.textContent = allTrending.length === 0
            ? "No trending data yet — start interacting with media on the home page!"
            : "No fast-rising media in this category yet.";
        return;
    }

    if (emptyMsg) emptyMsg.style.display = "none";
    statusEl.textContent = "";

    items.forEach(m => grid.appendChild(buildTrendingCard(m)));
}

// ─── Card Builders ────────────────────────────────────────────────────────────

function buildTop10Card(m, rank) {
    const card = document.createElement("div");
    card.className = "media-card";
    card.dataset.apiId = m.mediaApiId;
    card.dataset.type  = m.mediaType;
    card.dataset.title = m.title;

    const imgHtml = m.imageUrl
        ? `<img src="${escHtml(m.imageUrl)}" alt="${escHtml(m.title)}" class="media-thumbnail" loading="lazy" onerror="this.style.display='none'">`
        : `<div class="media-thumbnail-placeholder">No Image</div>`;

    const artistHtml = m.artist
        ? `<span class="artist-name">by ${escHtml(m.artist)}</span>`
        : "";

    card.innerHTML = `
        <span class="rank-number">${rank}</span>
        ${imgHtml}
        <div class="card-title">
            <strong>${escHtml(m.title)}</strong>
            ${artistHtml}
            <em>[${escHtml(m.mediaType)}]</em>
        </div>
        <div class="popularity-score">
            ⭐ Popularity Score: <span class="score-number">${m.totalScore}</span>
            <span class="score-detail">${m.likes} likes · ${m.views} views</span>
        </div>
    `;

    return card;
}

function buildTrendingCard(m) {
    const card = document.createElement("div");
    card.className = "media-card trending-card";
    card.dataset.title = m.title;
    card.dataset.type  = m.mediaType;

    const imgHtml = m.imageUrl
        ? `<img src="${escHtml(m.imageUrl)}" alt="${escHtml(m.title)}" class="media-thumbnail" loading="lazy" onerror="this.style.display='none'">`
        : `<div class="media-thumbnail-placeholder">No Image</div>`;

    const artistHtml = m.artist
        ? `<span class="artist-name">by ${escHtml(m.artist)}</span>`
        : "";

    card.innerHTML = `
        ${imgHtml}
        <div class="card-title">
            <strong>${escHtml(m.title)}</strong>
            ${artistHtml}
            <em>[${escHtml(m.mediaType)}]</em>
        </div>
        <div class="card-stats">
            <span>🔥 Fast-Rising Score: <span class="score-number">${m.weeklyLikes}</span> likes this week</span>
        </div>
    `;

    return card;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function setStatus(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}

function escHtml(str) {
    if (!str) return "";
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

// Records a VIEW interaction (called once on page load, not on every filter)
function recordView(mediaApiId, mediaType) {
    fetch(`/community/view?mediaApiId=${encodeURIComponent(mediaApiId)}&mediaType=${encodeURIComponent(mediaType)}`,
        { method: "POST" }
    ).catch(() => {});
}