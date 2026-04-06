/**
 * Liked Media Page
 *
 * Fetches all media the logged-in user has liked from /api/recommendations/liked,
 * renders them as cards (identical to the homepage rec cards), and supports:
 *   - Filtering by media type via tabs (All / Movies / Games / Songs)
 *   - Unliking: removes the card from the page immediately since it no longer belongs here
 *   - Clicking a card: records a VIEW interaction then navigates to the media detail page
 */

let allLikedItems = []; // cache the full list so tab-switching is instant (no re-fetch)
let currentFilter = "ALL";

// ─── Boot ────────────────────────────────────────────────────────────────────

document.addEventListener("DOMContentLoaded", () => {
    loadLikedMedia();

    document.querySelectorAll(".rec-tab").forEach(tab => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".rec-tab").forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            currentFilter = tab.dataset.type;
            renderFiltered();
        });
    });
});

// ─── Fetch ───────────────────────────────────────────────────────────────────

async function loadLikedMedia() {
    const statusEl = document.getElementById("likedStatus");
    const cardsEl  = document.getElementById("likedCards");

    statusEl.textContent = "Loading your liked media…";
    cardsEl.innerHTML    = "";

    try {
        const res = await fetch("/api/recommendations/liked");

        if (res.status === 401 || res.status === 403) {
            statusEl.textContent = "Please log in to see your liked media.";
            return;
        }

        const data = await res.json();

        if (!data || data.length === 0) {
            statusEl.textContent = "You haven't liked anything yet — head to the home page and start exploring!";
            return;
        }

        statusEl.textContent = "";
        allLikedItems = data;
        renderFiltered();

    } catch (err) {
        statusEl.textContent = "Could not load your liked media.";
        console.error(err);
    }
}

// ─── Render ──────────────────────────────────────────────────────────────────

function renderFiltered() {
    const cardsEl  = document.getElementById("likedCards");
    const statusEl = document.getElementById("likedStatus");
    cardsEl.innerHTML = "";

    const items = currentFilter === "ALL"
        ? allLikedItems
        : allLikedItems.filter(item => item.mediaType === currentFilter);

    if (items.length === 0) {
        statusEl.textContent = "No liked media in this category yet.";
        return;
    }

    statusEl.textContent = "";
    items.forEach(item => cardsEl.appendChild(buildCard(item)));
}

// Builds a single media card — identical structure to the homepage rec cards.
function buildCard(item) {
    const card = document.createElement("a");
    card.className = "rec-card";
    if (item.mediaType === "SONG") card.classList.add("song-card");

    card.dataset.mediaApiId = item.mediaApiId;
    card.dataset.mediaType  = item.mediaType;
    card.dataset.genre      = item.genre;

    const img = item.mediaType === "SONG"
        ? ""
        : item.imageUrl && item.imageUrl !== "null"
            ? `<img class="rec-img" src="${item.imageUrl}" alt="${item.title}" loading="lazy" onerror="this.style.display='none'">`
            : `<div class="rec-img rec-img-placeholder">No Image</div>`;

    let destination; 
        if (item.mediaType === 'SONG') {
          destination = `/mediaView/song/${encodeURIComponent(item.artist)}/${encodeURIComponent(item.title)}`; 
        } else { 
          destination = `/mediaView/${mediaType}/${item.mediaApiId}`;
        }
    card.href = "javascript:void(0)";
    card.addEventListener("click", async (e) => {
        if (e.target.closest(".rec-btn")) return;
        e.preventDefault();
        await recordInteraction(card, "VIEW");
        window.location.href = destination;
    });

    card.innerHTML = `
        ${img}
        <div class="rec-info">
            <p class="rec-title">${item.title}</p>
            <p class="rec-meta">${item.genre}${item.mediaType === "SONG" && item.artist ? " · " + item.artist : ""}</p>
        </div>
        <div class="rec-actions">
            <button class="rec-btn like-btn liked" data-liked="true" onclick="toggleLike(this)">❤ Liked!</button>
        </div>
    `;

    return card;
}

// ─── Like / Unlike ───────────────────────────────────────────────────────────

// On this page, unliking a card removes it entirely from the list and the DOM
// (unlike the homepage where it just toggles the button state).
async function toggleLike(btn) {
    const card       = btn.closest(".rec-card");
    const mediaApiId = card.dataset.mediaApiId;
    const mediaType  = card.dataset.mediaType;
    const genre      = card.dataset.genre;
    const isLiked    = btn.dataset.liked === "true";

    if (isLiked) {
        // Unlike — remove from this page
        try {
            const res = await fetch(`/api/recommendations/interactions/like?mediaApiId=${encodeURIComponent(mediaApiId)}`, {
                method: "DELETE"
            });
            if (res.ok) {
                // Remove from cached list
                allLikedItems = allLikedItems.filter(i => i.mediaApiId !== mediaApiId);
                // Remove card from DOM with a fade-out
                card.style.transition = "opacity 0.3s ease";
                card.style.opacity = "0";
                setTimeout(() => {
                    card.remove();
                    // Show empty message if nothing left in current filter
                    const cardsEl  = document.getElementById("likedCards");
                    const statusEl = document.getElementById("likedStatus");
                    if (cardsEl.children.length === 0) {
                        statusEl.textContent = allLikedItems.length === 0
                            ? "You haven't liked anything yet — head to the home page and start exploring!"
                            : "No liked media in this category yet.";
                    }
                }, 300);
            } else {
                console.warn("Unlike failed:", res.status);
            }
        } catch (err) {
            console.error("Unlike error:", err);
        }
    } else {
        // Re-like (shouldn't normally happen on this page, but handle gracefully)
        try {
            const res = await fetch("/api/recommendations/interactions", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    mediaApiId,
                    interactionType: "LIKE",
                    mediaType,
                    genres: [genre]
                })
            });
            if (res.ok) {
                btn.textContent  = "❤ Liked!";
                btn.classList.add("liked");
                btn.dataset.liked = "true";
            } else {
                console.warn("Like failed:", res.status);
            }
        } catch (err) {
            console.error("Like error:", err);
        }
    }
}

// ─── View Interaction ────────────────────────────────────────────────────────

async function recordInteraction(card, interactionType) {
    const mediaApiId = card.dataset.mediaApiId;
    const mediaType  = card.dataset.mediaType;
    const genre      = card.dataset.genre;

    try {
        const res = await fetch("/api/recommendations/interactions", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                mediaApiId,
                interactionType,
                mediaType,
                genres: [genre]
            })
        });
        if (!res.ok) console.warn("Interaction failed:", res.status);
    } catch (err) {
        console.error("Interaction error:", err);
    }
}