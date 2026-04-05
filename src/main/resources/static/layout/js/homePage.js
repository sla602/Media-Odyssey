/**
 * Board Cards Horizontal Scroll with Navigation
 * Initializes scroll containers with navigation arrows that appear on hover
 */

document.addEventListener('DOMContentLoaded', function() {
  initBoardsScroll();
});

function initBoardsScroll() {
  const container = document.getElementById('personalBoardsContainer');
  if (!container) return;

  const createBoard = container.querySelector('.create-board');
  const boardCards = container.querySelectorAll('.board-card');
  const title = container.querySelector('.personal-boards-title');

  if (createBoard) createBoard.remove();
  boardCards.forEach(card => card.remove());

  const scrollWrapper = document.createElement('div');
  scrollWrapper.className = 'boards-scroll-wrapper';

  const scrollContainer = document.createElement('div');
  scrollContainer.className = 'boards-scroll-container';

  const prevBtn = document.createElement('button');
  prevBtn.className = 'scroll-nav-btn prev';
  prevBtn.innerHTML = `
    <svg viewBox="0 0 24 24">
      <polyline points="15 18 9 12 15 6"></polyline>
    </svg>
  `;
  prevBtn.setAttribute('aria-label', 'Scroll left');

  const nextBtn = document.createElement('button');
  nextBtn.className = 'scroll-nav-btn next';
  nextBtn.innerHTML = `
    <svg viewBox="0 0 24 24">
      <polyline points="9 18 15 12 9 6"></polyline>
    </svg>
  `;
  nextBtn.setAttribute('aria-label', 'Scroll right');

  if (createBoard) {
    scrollContainer.appendChild(createBoard);
  }

  boardCards.forEach(card => {
    scrollContainer.appendChild(card);
  });

  scrollWrapper.appendChild(prevBtn);
  scrollWrapper.appendChild(scrollContainer);
  scrollWrapper.appendChild(nextBtn);

  if (title) {
    title.after(scrollWrapper);
  } else {
    container.appendChild(scrollWrapper);
  }

  const scrollAmount = 240;

  prevBtn.addEventListener('click', () => {
    scrollContainer.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
  });

  nextBtn.addEventListener('click', () => {
    scrollContainer.scrollBy({ left: scrollAmount, behavior: 'smooth' });
  });

  function updateNavButtons() {
    const { scrollLeft, scrollWidth, clientWidth } = scrollContainer;
    const maxScroll = scrollWidth - clientWidth;
    if (scrollLeft > 10) {
      prevBtn.classList.add('visible');
    } else {
      prevBtn.classList.remove('visible');
    }
    if (scrollLeft < maxScroll - 10) {
      nextBtn.classList.add('visible');
    } else {
      nextBtn.classList.remove('visible');
    }
  }

  updateNavButtons();
  scrollContainer.addEventListener('scroll', updateNavButtons);
  window.addEventListener('resize', updateNavButtons);

  scrollContainer.setAttribute('tabindex', '0');
  scrollContainer.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowLeft') {
      scrollContainer.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
    } else if (e.key === 'ArrowRight') {
      scrollContainer.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }
  });

  let isDown = false;
  let startX;
  let scrollLeftStart;

  scrollContainer.addEventListener('mousedown', (e) => {
    if (e.target === scrollContainer) {
      isDown = true;
      scrollContainer.style.cursor = 'grabbing';
      startX = e.pageX - scrollContainer.offsetLeft;
      scrollLeftStart = scrollContainer.scrollLeft;
    }
  });

  scrollContainer.addEventListener('mouseleave', () => {
    isDown = false;
    scrollContainer.style.cursor = 'grab';
  });

  scrollContainer.addEventListener('mouseup', () => {
    isDown = false;
    scrollContainer.style.cursor = 'grab';
  });

  scrollContainer.addEventListener('mousemove', (e) => {
    if (!isDown) return;
    e.preventDefault();
    const x = e.pageX - scrollContainer.offsetLeft;
    const walk = (x - startX) * 1.5;
    scrollContainer.scrollLeft = scrollLeftStart - walk;
  });
}

/* =========================================================
   RECOMMENDATIONS
   ========================================================= */

let currentMediaType = "MOVIE";

// Cache: only used by prefetch so the first-ever load of each tab is instant
// if the background fetch finished before the user clicks. Tab clicks always
// clear this so they get fresh recommendations, not stale cached ones.
const recCache = {};

// Tracks in-flight user-initiated requests (tab clicks + initial load)
const recInFlight = {};

// Tracks in-flight background prefetch requests — separate from recInFlight
// so a running prefetch never blocks loadRecommendations on a tab click
const prefetchInFlight = new Set();

async function loadRecommendations(mediaType) {
    const statusEl = document.getElementById("recStatus");
    const cardsEl  = document.getElementById("recCards");

    // Serve from cache if available (only hits on first load if prefetch finished)
    if (recCache[mediaType]) {
        statusEl.textContent = "";
        cardsEl.innerHTML = "";
        renderCards(recCache[mediaType]);
        return;
    }

    if (recInFlight[mediaType]) return;
    recInFlight[mediaType] = true;

    statusEl.textContent = "Loading… wait time is 30 seconds max. Thank you for your patience!";
    cardsEl.innerHTML    = "";

    try {
        const res = await fetch(`/api/recommendations?mediaType=${mediaType}`);

        if (res.status === 401 || res.status === 403) {
            statusEl.textContent = "Please log in to see recommendations.";
            return;
        }

        const data = await res.json();

        if (!data || data.length === 0) {
            statusEl.textContent =
                "No recommendations yet — browse some media first so we can learn your taste!";
            return;
        }

        recCache[mediaType] = data;

        // Only render if this type is still the active tab when the response arrives
        if (mediaType === currentMediaType) {
            statusEl.textContent = "";
            renderCards(data);
        }

    } catch (err) {
        if (mediaType === currentMediaType) {
            statusEl.textContent = "Could not load recommendations.";
        }
        console.error(err);
    } finally {
        recInFlight[mediaType] = false;
    }
}

// Silently fetch and cache without touching the DOM
async function prefetchRecommendations(mediaType) {
    if (recCache[mediaType] || prefetchInFlight.has(mediaType)) return;
    prefetchInFlight.add(mediaType);
    try {
        const res = await fetch(`/api/recommendations?mediaType=${mediaType}`);
        if (!res.ok) return;
        const data = await res.json();
        if (data && data.length > 0) {
            recCache[mediaType] = data;
        }
    } catch (err) {
        console.error(`Prefetch failed for ${mediaType}:`, err);
    } finally {
        prefetchInFlight.delete(mediaType);
    }
}

function renderCards(items) {
    const cardsEl = document.getElementById("recCards");
    
    items.sort(() => Math.random() - 0.5); // shuffle
    items.forEach(item => {
        const card = document.createElement("a");
        card.className = "rec-card";
        if (item.mediaType === 'SONG') {
            card.classList.add('song-card');
        }
        card.dataset.mediaApiId = item.mediaApiId;
        card.dataset.mediaType  = item.mediaType;
        card.dataset.genre      = item.genre;

        const img = item.mediaType === 'SONG'
            ? ''
            : item.imageUrl && item.imageUrl !== "null"
                ? `<img class="rec-img" src="${item.imageUrl}" alt="${item.title}" loading="lazy" onerror="this.style.display='none'">`
                : `<div class="rec-img rec-img-placeholder">No Image</div>`;

        const mediaType = item.mediaType.toLowerCase();
        // Adjust the destination URL so backend can fetch the artist and track name correctly for last FM
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
                <p class="rec-meta">${item.genre}${item.mediaType === 'SONG' && item.artist ? ' · ' + item.artist : ''}</p>
            </div>
            <div class="rec-actions">
                <button class="rec-btn like-btn" onclick="toggleLike(this)">♡ Like</button>
            </div>
        `;

        if (item.userLiked) {
            const likeBtn = card.querySelector(".like-btn");
            likeBtn.textContent = "❤ Liked!";
            likeBtn.classList.add("liked");
            likeBtn.dataset.liked = "true";
        }

        cardsEl.appendChild(card);
    });
}

async function toggleLike(btn) {
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
                btn.textContent = "♡ Like";
                btn.classList.remove("liked");
                btn.dataset.liked = "false";
            } else {
                console.warn("Unlike failed:", res.status);
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
                    genres: [genre],
                    title: card.querySelector(".rec-title")?.textContent || "",
                    artist: card.querySelector(".rec-meta")?.textContent?.split(" · ")[1] || "",
                    imageUrl: card.querySelector(".rec-img")?.src || ""
                })
            });
            if (res.ok) {
                btn.textContent = "❤ Liked!";
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

async function recordInteraction(btnOrCard, interactionType) {
    const card = btnOrCard.classList.contains("rec-card")
        ? btnOrCard
        : btnOrCard.closest(".rec-card");

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
        if (!res.ok) {
            console.warn("Interaction failed:", res.status);
        }
    } catch (err) {
        console.error("Interaction error:", err);
    }
}

// Tab switching — clear cache for the clicked type so it always fetches fresh
document.querySelectorAll(".rec-tab").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".rec-tab").forEach(t => t.classList.remove("active"));
        tab.classList.add("active");
        currentMediaType = tab.dataset.type;
        delete recCache[currentMediaType]; // force fresh fetch on every tab click
        loadRecommendations(currentMediaType);
    });
});

// Start all three fetches simultaneously — MOVIE renders when done,
// GAME and SONG go straight to cache so first click is instant
loadRecommendations(currentMediaType);
["GAME", "SONG"].forEach(type => prefetchRecommendations(type));