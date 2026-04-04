/* ===========================================
          Star Rating System Functions:
=============================================*/

const stars = document.querySelectorAll('.star');
let currentRating = 0;

stars.forEach((star, index) => {
  star.addEventListener('click', () => {
    currentRating = index + 1;
    updateStars();
  });

  star.addEventListener('mouseenter', () => {
    highlightStars(index + 1);
  });
});



function highlightStars(count) {
  stars.forEach((star, index) => {
    star.classList.toggle('active', index < count);
  });
}

function updateStars() {
  highlightStars(currentRating);
}


// Like Button Toggle
/*
const likeBtn = document.getElementById('like-btn');
likeBtn.addEventListener('click', () => {
  likeBtn.classList.toggle('active');
});

// Watched Button Toggle
const watchedBtn = document.getElementById('watched-btn');
const watchedIcon = document.getElementById('watched-icon');
const watchedSvg = document.getElementById('watched-svg');

// Hide SVG if custom image is provided
if (watchedIcon.src && watchedIcon.src !== window.location.href) {
  watchedSvg.style.display = 'none';
} else {
  watchedIcon.style.display = 'none';
}

watchedBtn.addEventListener('click', () => {
  watchedBtn.classList.toggle('watched-active');
});
*/

/*======================================================== 
                  ADD MOVIE INTO BOARD JS
                      DROP DOWN MENU
==========================================================*/
document.addEventListener("DOMContentLoaded", function () {

  const dropdownBtn = document.getElementById("dropdownBtn");
  const dropdownMenu = document.getElementById("dropdownMenu");
  const movieId = document.getElementById("movieApiId")?.value;

  if (!dropdownBtn || !dropdownMenu) return;

  // Toggle dropdown
  dropdownBtn.addEventListener("click", function (e) {
    e.stopPropagation();
    dropdownMenu.classList.toggle("show");
  });

  // Close when clicking outside
  document.addEventListener("click", function (e) {
    if (!dropdownMenu.contains(e.target) && !dropdownBtn.contains(e.target)) {
      dropdownMenu.classList.remove("show");
    }
  });

  // Handle clicking board
  const dropdownItems = document.querySelectorAll(".dropdown-item");

  console.log("Boards found:", dropdownItems.length);

  dropdownItems.forEach(button => {
    button.addEventListener("click", function () {

      const boardId = this.dataset.boardId;

      if (!boardId || !movieId) return;

      fetch(`/api/boards/${boardId}/media`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        }, 
        body: JSON.stringify({
          mediaApiId: movieId
        })
      })
      .then(response => {
        if (response.ok) {
          alert("Media added to board!");
        } else {
          alert("Already exists or error occurred.");
        }
      });

      dropdownMenu.classList.remove("show");
    });
  });

});