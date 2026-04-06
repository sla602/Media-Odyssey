/* =========================================================
   SEARCH PAGE
   ========================================================= */

let currentType = initialType || "MOVIE";

// Run search on page load if query is present
if (initialQuery && initialQuery.trim() !== "") {
    runSearch(initialQuery, currentType);
}

// Tab switching
document.querySelectorAll(".rec-tab").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".rec-tab").forEach(t => t.classList.remove("active"));
        tab.classList.add("active");
        currentType = tab.dataset.type;
        const q = new URLSearchParams(window.location.search).get("q") || "";
        if (q.trim()) runSearch(q, currentType);
    });
});

async function runSearch(query, type) {
    const statusEl = document.getElementById("searchStatus");
    const cardsEl  = document.getElementById("searchCards");

    statusEl.textContent = "Searching…";
    cardsEl.innerHTML    = "";

    try {
        const res  = await fetch(`/api/search?q=${encodeURIComponent(query)}&type=${type}`);
        const data = await res.json();

        if (!data || data.length === 0) {
            statusEl.textContent = "No results found.";
            return;
        }

        statusEl.textContent = "";
        renderSearchCards(data);

    } catch (err) {
        statusEl.textContent = "Search failed. Please try again.";
        console.error(err);
    }
}

function renderSearchCards(items) {
    const cardsEl = document.getElementById("searchCards");

    items.forEach(item => {
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

        const mediaType   = item.mediaType.toLowerCase();
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
            await recordSearchInteraction(card, "VIEW");
            window.location.href = destination;
        });

        const scoreLine = item.mediaType === "SONG"
            ? item.artist
            : "";

        // if already liked, render the button in liked state immediately
        const likedClass = item.userLiked ? " liked" : "";
        const likedText  = item.userLiked ? "❤ Liked!" : "♡ Like";
        const likedData  = item.userLiked ? "true" : "false";

        card.innerHTML = `
            ${img}
            <div class="rec-info">
                <p class="rec-title">${item.title}</p>
                <p class="rec-meta">${item.genre}${scoreLine ? " · " + scoreLine : ""}</p>
            </div>
            <div class="rec-actions">
                <button class="rec-btn like-btn${likedClass}" data-liked="${likedData}" onclick="toggleSearchLike(this)">${likedText}</button>
            </div>
        `;

        cardsEl.appendChild(card);
    });
}

async function toggleSearchLike(btn) {
    const card       = btn.closest(".rec-card");
    const mediaApiId = card.dataset.mediaApiId;
    const mediaType  = card.dataset.mediaType;
    const genre      = card.dataset.genre;
    const isLiked    = btn.dataset.liked === "true";

    if (isLiked) {
        try {
            const res = await fetch(`/api/recommendations/interactions/like?mediaApiId=${encodeURIComponent(mediaApiId)}`, {
                method: "DELETE"
            });
            if (res.ok) {
                btn.textContent   = "♡ Like";
                btn.classList.remove("liked");
                btn.dataset.liked = "false";
            }
        } catch (err) {
            console.error("Unlike error:", err);
        }
    } else {
        try {
            const res = await fetch("/api/recommendations/interactions", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    mediaApiId,
                    interactionType: "LIKE",
                    mediaType,
                    genres: genre ? [genre] : [],
                    title: card.querySelector(".rec-title")?.textContent || "",
                    artist: card.querySelector(".rec-meta")?.textContent?.split(" · ")[1] || "",
                    imageUrl: card.querySelector(".rec-img")?.src || ""
                })
            });
            if (res.ok) {
                btn.textContent   = "❤ Liked!";
                btn.classList.add("liked");
                btn.dataset.liked = "true";
            }
        } catch (err) {
            console.error("Like error:", err);
        }
    }
}

async function recordSearchInteraction(card, interactionType) {
    const mediaApiId = card.dataset.mediaApiId;
    const mediaType  = card.dataset.mediaType;
    const genre      = card.dataset.genre;

    try {
        await fetch("/api/recommendations/interactions", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                mediaApiId,
                interactionType,
                mediaType,
                genres: genre ? [genre] : []  // empty array = interaction saved, no genre rows written
            })
        });
    } catch (err) {
        console.error("Interaction error:", err);
    }
}