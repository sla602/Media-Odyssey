/**
 * Community Favourites Page — trending.js
 *
 * Fetches per-category ranking data from /community/data (10 top per category, 5 trending per category),
 * then filters client-side when the user clicks a category tab.
 *
 * This avoids repeated server round-trips while keeping all category data in memory.
 */

let top10ByCategory = {};  // {MOVIE: [...], GAME: [...], SONG: [...]}
let trendingByCategory = {};  // {MOVIE: [...], GAME: [...], SONG: [...]}
let currentFilter = "ALL";
const viewedMediaKeys = new Set();  // track viewed media to prevent duplicate view recording

// ─── Boot ─────────────────────────────────────────────────────────────────────

document.addEventListener("DOMContentLoaded", () => {
    loadCommunityData();

    document.querySelectorAll(".tab-btn").forEach(tab => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".tab-btn").forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            currentFilter = normalizeMediaType(tab.dataset.type);
            renderFiltered();
        });
    });
});

// ─── Fetch ────────────────────────────────────────────────────────────────────

async function loadCommunityData() {
    setStatus("top10Status", "Loading…");
    setStatus("trendingStatus", "Loading…");

    try {
        const res = await fetch("/community/data");

        if (!res.ok) {
            setStatus("top10Status", "Could not load ranking data.");
            setStatus("trendingStatus", "Could not load trending data.");
            return;
        }

        const data = await res.json();
        top10ByCategory = data.top10 || {};
        trendingByCategory = data.trending || {};

        setStatus("top10Status", "");
        setStatus("trendingStatus", "");

        renderFiltered();
    } catch (err) {
        setStatus("top10Status", "Could not load ranking data.");
        setStatus("trendingStatus", "Could not load trending data.");
        console.error("Community data fetch error:", err);
    }
}

// ─── Render ───────────────────────────────────────────────────────────────────

function renderFiltered() {
    const normalizedFilter = normalizeMediaType(currentFilter);

    // Get items for current category/filter from pre-fetched data
    let top10Items, trendingItems;

    // Use "ALL" key for cross-category top 5, or specific category
    top10Items = top10ByCategory[normalizedFilter] || [];
    trendingItems = trendingByCategory[normalizedFilter] || [];

    renderTop10(top10Items);
    renderTrending(trendingItems);
}

function renderTop10(items) {
    const grid = document.getElementById("top10Grid");
    const emptyMsg = document.getElementById("top10Empty");
    const statusEl = document.getElementById("top10Status");

    grid.innerHTML = "";

    if (items.length === 0) {
        if (emptyMsg) emptyMsg.style.display = "block";
        const hasAnyTop10 = Object.values(top10ByCategory).some(arr => arr && arr.length > 0);
        statusEl.textContent = !hasAnyTop10
            ? "No rankings yet — start interacting with media on the home page!"
            : "No ranked media in this category yet.";
        return;
    }

    if (emptyMsg) emptyMsg.style.display = "none";
    statusEl.textContent = "";

    items.forEach((m, idx) => grid.appendChild(buildTop10Card(m, idx + 1)));
    recordVisibleMediaViews(items);
}

function renderTrending(items) {
    const grid = document.getElementById("trendingGrid");
    const emptyMsg = document.getElementById("trendingEmpty");
    const statusEl = document.getElementById("trendingStatus");

    grid.innerHTML = "";

    if (items.length === 0) {
        if (emptyMsg) emptyMsg.style.display = "block";
        const hasAnyTrending = Object.values(trendingByCategory).some(arr => arr && arr.length > 0);
        statusEl.textContent = !hasAnyTrending
            ? "No trending data yet — start interacting with media on the home page!"
            : "No fast-rising media in this category yet.";
        return;
    }

    if (emptyMsg) emptyMsg.style.display = "none";
    statusEl.textContent = "";

    items.forEach(m => grid.appendChild(buildTrendingCard(m)));
    recordVisibleMediaViews(items);
}

// ─── Card Builders ────────────────────────────────────────────────────────────

function buildTop10Card(m, rank) {
    const card = document.createElement("div");
    card.className = "media-card";
    card.dataset.apiId = m.mediaApiId;
    card.dataset.type = m.mediaType;
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
    card.dataset.type = m.mediaType;

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

// Normalize media type to canonical form (MOVIE | GAME | SONG | ALL)
function normalizeMediaType(type) {
    if (!type) return "ALL";
    const upper = String(type).toUpperCase().trim();
    if (upper === "MOVIE" || upper === "GAME" || upper === "SONG") return upper;
    return "ALL";
}

// Records VIEW interactions only for rendered items, deduplicated by media key
function recordVisibleMediaViews(items) {
    items.forEach(m => {
        const viewKey = `${normalizeMediaType(m.mediaType)}|${m.mediaApiId}`;
        if (viewedMediaKeys.has(viewKey)) return;
        viewedMediaKeys.add(viewKey);
        recordView(m.mediaApiId, m.mediaType);
    });
}

// Records a VIEW interaction for a specific media item
function recordView(mediaApiId, mediaType) {
    fetch(`/community/view?mediaApiId=${encodeURIComponent(mediaApiId)}&mediaType=${encodeURIComponent(mediaType)}`,
        { method: "POST" }
    ).catch(() => { });
}
