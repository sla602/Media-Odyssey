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

  //=============== Handle clicking board and sending media obj to backend ===================
  const dropdownItems = document.querySelectorAll(".dropdown-item");

  console.log("Boards found:", dropdownItems.length);

  dropdownItems.forEach(button => {
    button.addEventListener("click", function (e) {
      e.stopPropagation();
      const boardId = button.dataset.boardId; // get the id of the board being clicked on in drop down menu
      const mediaType = dropdownBtn.dataset.type; 

      let payload = {}; 

      if (mediaType === 'movie' || mediaType === 'game') {
        payload = {
          type: mediaType, 
          mediaApiId: dropdownBtn.dataset.id
        };
      }

      if (mediaType === 'music') {
        payload = {
          type: mediaType,
          artist: dropdownBtn.dataset.artist,
          track: dropdownBtn.dataset.track
        };
      }

      addToBoard(boardId, payload);
    });
  }); 

});

async function addToBoard(boardId, media) {
    try {
        const response = await fetch(`/api/boards/${boardId}/media`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(media)
        });

        const result = await response.json();

        if (response.ok) {
            showPopup(result.message || "Added successfully!", "success");
        } else {
            showPopup(result.message || "Failed to add media", "error");
        }

    } catch (error) {
        console.error("Network error:", error);
        showPopup("Network error. Please try again.", "error");
    }
}

// Change from browser alert to pop up message 
function showPopup(message, type) {
    let popup = document.createElement("div");
    popup.className = `custom-popup ${type}`;
    popup.innerText = message;

    document.body.appendChild(popup);

    setTimeout(() => {
        popup.classList.add("show");
    }, 10);

    setTimeout(() => {
        popup.classList.remove("show");
        setTimeout(() => popup.remove(), 300);
    }, 2500);
}