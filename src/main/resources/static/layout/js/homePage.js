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

  // Get all board elements (create button + board cards)
  const createBoard = container.querySelector('.create-board');
  const boardCards = container.querySelectorAll('.board-card');
  const title = container.querySelector('.personal-boards-title');

  // Remove existing board elements from container (keep title)
  if (createBoard) createBoard.remove();
  boardCards.forEach(card => card.remove());

  // Create the scroll wrapper structure
  const scrollWrapper = document.createElement('div');
  scrollWrapper.className = 'boards-scroll-wrapper';

  const scrollContainer = document.createElement('div');
  scrollContainer.className = 'boards-scroll-container';

  // Create navigation buttons
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

  // Add create board button first
  if (createBoard) {
    scrollContainer.appendChild(createBoard);
  }

  // Add all board cards
  boardCards.forEach(card => {
    scrollContainer.appendChild(card);
  });

  // Assemble the wrapper
  scrollWrapper.appendChild(prevBtn);
  scrollWrapper.appendChild(scrollContainer);
  scrollWrapper.appendChild(nextBtn);

  // Insert after title
  if (title) {
    title.after(scrollWrapper);
  } else {
    container.appendChild(scrollWrapper);
  }

  // Scroll amount per click (card width + gap)
  const scrollAmount = 240;

  // Navigation click handlers
  prevBtn.addEventListener('click', () => {
    scrollContainer.scrollBy({
      left: -scrollAmount,
      behavior: 'smooth'
    });
  });

  nextBtn.addEventListener('click', () => {
    scrollContainer.scrollBy({
      left: scrollAmount,
      behavior: 'smooth'
    });
  });

  // Update button visibility based on scroll position
  function updateNavButtons() {
    const { scrollLeft, scrollWidth, clientWidth } = scrollContainer;
    const maxScroll = scrollWidth - clientWidth;

    // Show/hide prev button
    if (scrollLeft > 10) {
      prevBtn.classList.add('visible');
    } else {
      prevBtn.classList.remove('visible');
    }

    // Show/hide next button
    if (scrollLeft < maxScroll - 10) {
      nextBtn.classList.add('visible');
    } else {
      nextBtn.classList.remove('visible');
    }
  }

  // Initial check
  updateNavButtons();

  // Update on scroll
  scrollContainer.addEventListener('scroll', updateNavButtons);

  // Update on window resize
  window.addEventListener('resize', updateNavButtons);

  // Add keyboard navigation support
  scrollContainer.setAttribute('tabindex', '0');
  scrollContainer.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowLeft') {
      scrollContainer.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
    } else if (e.key === 'ArrowRight') {
      scrollContainer.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }
  });

  // Touch/drag scroll support for mobile
  let isDown = false;
  let startX;
  let scrollLeftStart;

  scrollContainer.addEventListener('mousedown', (e) => {
    // Only enable drag if clicking on empty space
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

// Fetch recommendations for the selected media type
async function loadRecommendations(mediaType) {
    const statusEl = document.getElementById("recStatus");
    const cardsEl  = document.getElementById("recCards");

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

        statusEl.textContent = "";
        renderCards(data);

    } catch (err) {
        statusEl.textContent = "Could not load recommendations.";
        console.error(err);
    }
}

// Build a card for each recommendation
function renderCards(items) {
    const cardsEl = document.getElementById("recCards");

    /*
        *** This <div> HTML will be modified so user can click on them and be brought to HTML that displays
        **  details information of the meta they clicked on. 
        ** Logic: Media display Controller will have the path /mediaView/mediaType/mediaId
        *  This ensures the type of media being cliked on and its id. (nice =])
        *
        *** Only movie is being worked on right now.
    */
    items.sort(() => Math.random() - 0.5); // shuffle
    items.forEach(item => {
        const card = document.createElement("a");
        card.className = "rec-card";
        card.dataset.mediaApiId = item.mediaApiId;
        card.dataset.mediaType  = item.mediaType;
        card.dataset.genre      = item.genre;

        const img = item.imageUrl && item.imageUrl !== "null"
            ? `<img class="rec-img" src="${item.imageUrl}" alt="${item.title}" onerror="this.style.display='none'">`
            : `<div class="rec-img rec-img-placeholder">No Image</div>`;

        // Intercept click: record VIEW interaction then navigate
        const mediaType = item.mediaType.toLowerCase();
        const destination = `/mediaView/${mediaType}/${item.mediaApiId}`;
        card.href = "javascript:void(0)";
        card.addEventListener("click", async (e) => {
            // Allow like button clicks to bubble without triggering navigation
            if (e.target.closest(".rec-btn")) return;
            e.preventDefault();
            await recordInteraction(card, "VIEW");
            window.location.href = destination;
        });

        card.innerHTML = `
            ${img}
            <div class="rec-info">
                <p class="rec-title">${item.title}</p>
                <p class="rec-meta">${item.genre} · ${item.mediaType === 'SONG' ? item.artist : 'score: ' + item.score.toFixed(1)}</p>
            </div>
            <div class="rec-actions">
                <button class="rec-btn like-btn" onclick="recordInteraction(this, 'LIKE')">♡ Like</button>
            </div>
        `;

        // If user already liked this item, show it as liked immediately
        if (item.userLiked) {
            const likeBtn = card.querySelector(".like-btn");
            likeBtn.textContent = "❤ Liked!";
            likeBtn.classList.add("liked");
            likeBtn.disabled = true;
        }

        cardsEl.appendChild(card);
    });
}

// POST a VIEW or LIKE interaction to the backend
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

        if (res.ok) {
            if (interactionType === "LIKE") {
                btnOrCard.textContent = "❤ Liked!";
                btnOrCard.classList.add("liked");
                btnOrCard.disabled = true;
            }
        } else {
            console.warn("Interaction failed:", res.status);
        }
    } catch (err) {
        console.error("Interaction error:", err);
    }
}

// Tab switching
document.querySelectorAll(".rec-tab").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".rec-tab").forEach(t => t.classList.remove("active"));
        tab.classList.add("active");
        currentMediaType = tab.dataset.type;
        loadRecommendations(currentMediaType);
    });
});

// Load on page open
loadRecommendations(currentMediaType);