// =================== Board Display Page =================================================

document.addEventListener('DOMContentLoaded', function() {

    // =============== Choose either POSTS or MEDIA =====================================
    const postsBtn = document.getElementById('postsBtn');
    const mediaBtn = document.getElementById('mediaBtn');
    const postsContainer = document.getElementById('postsContainer');
    const mediaContainer = document.getElementById('mediaContainer');

    // Tab switching 
    if (postsBtn && mediaBtn) {
        postsBtn.addEventListener('click', function() {
            // Update button states
            postsBtn.classList.add('active');
            mediaBtn.classList.remove('active');
            
            // Show/hide containers
            postsContainer.style.display = 'block';
            mediaContainer.style.display = 'none';
        });

        mediaBtn.addEventListener('click', function() {
            // Update button states
            mediaBtn.classList.add('active');
            postsBtn.classList.remove('active');
            
            // Show/hide containers
            mediaContainer.style.display = 'block';
            postsContainer.style.display = 'none';
        });
    }

    // ======================= Specific media selector ====================================================

    // ============= Tab switching logic for media =======================================
    const moviesTab = document.getElementById('moviesTab');
    const musicTab = document.getElementById('musicTab');
    const gamesTab = document.getElementById('gamesTab');

    const moviesSection = document.getElementById('moviesSection');
    const musicSection = document.getElementById('musicSection');
    const gamesSection = document.getElementById('gamesSection');

    function switchMediaTab(activeTab, activeSection) {
        // remove all active
        document.querySelectorAll('.media-tab').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.media-section').forEach(sec => sec.style.display = 'none');

        // activate selected
        activeTab.classList.add('active');
        activeSection.style.display = 'block';
    }

    if (moviesTab) {
        moviesTab.addEventListener('click', () => switchMediaTab(moviesTab, moviesSection)); }
    if (musicTab) {
        musicTab.addEventListener('click', () => switchMediaTab(musicTab, musicSection)); } 
    if (gamesTab) {
        gamesTab.addEventListener('click', () => switchMediaTab(gamesTab, gamesSection)); }

    // ============= Display (saved) media : fetching media to display ==================

    const boardId = window.location.pathname.split('/').pop();
    function createMediaCard(item, type) {
        const card = document.createElement('div');
        card.classList.add('media-card');

        let link = '';

        if (type === 'movie') {
            link = `/mediaView/movie/${item.id}`;
        } else if (type === 'game') {
            link = `/mediaView/game/${item.id}`;
        } else if (type === 'music') {
            link = `/mediaView/music?artist=${encodeURIComponent(item.artist)}&track=${encodeURIComponent(item.title)}`;
        }

        card.innerHTML = `
            <a href="${link}">
                <img src="${getImage(item, type)}" alt="${getTitle(item, type)}">
                <h4>${getTitle(item, type)}</h4>
                ${type === 'music' ? `<p class="media-extra">${item.artist}</p>` : ''}
            </a>
        `;

        return card;
    }

    // HELPERS (normalize fields)
    function getTitle(item, type) {
        if (type === 'movie') return item.title;
        if (type === 'game') return item.name;
        if (type === 'music') return item.title;
    }

    function getImage(item, type) {
        if (type === 'movie') return `https://image.tmdb.org/t/p/w500${item.poster_path}`;
        if (type === 'game') return item.background_image;
        if (type === 'music') return item.poster_path;
    }

    function renderMedia(items, container, type) {
    container.innerHTML = '';

        if (!items || items.length === 0) {
            container.innerHTML = '<p class="empty-message">Nothing here yet</p>';
            return;
        }

        items.forEach(item => {
            container.appendChild(createMediaCard(item, type));
        });
    }

    // functions for fetching
    function loadMovies() {
        fetch(`/boards/display/${boardId}/movies`)
            .then(res => res.json())
            .then(data => renderMedia(data, moviesSection, 'movie'))
            .catch(err => {
                console.error('Movies error:', err);
                moviesSection.innerHTML = '<p class="empty-message">Failed to load movies</p>';
            });
    }

    function loadGames() {
        fetch(`/boards/display/${boardId}/games`)
            .then(res => res.json())
            .then(data => renderMedia(data, gamesSection, 'game'))
            .catch(err => {
                console.error('Games error:', err);
                gamesSection.innerHTML = '<p class="empty-message">Failed to load games</p>';
            });
    }

    function loadMusic() {
        fetch(`/boards/display/${boardId}/music`)
            .then(res => res.json())
            .then(data => renderMedia(data, musicSection, 'music'))
            .catch(err => {
                console.error('Music error:', err);
                musicSection.innerHTML = '<p class="empty-message">Failed to load music</p>';
            });
    }

    // INITIAL LOAD
    loadMovies(); // default
    moviesTab.addEventListener('click', loadMovies);
    musicTab.addEventListener('click', loadMusic);
    gamesTab.addEventListener('click', loadGames);
});
