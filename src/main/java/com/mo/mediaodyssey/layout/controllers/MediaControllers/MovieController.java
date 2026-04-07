package com.mo.mediaodyssey.layout.controllers.MediaControllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.mo.mediaodyssey.layout.DTO.MoviesTMDB.MovieResponse;
import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.services.BoardsService;
import com.mo.mediaodyssey.layout.services.MediaServices.MovieService;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mediaView/movie")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private BoardsService boardsService;

    @Autowired
    private CurrentAccountService currentAccountService;

    /*
     * *** This function will fetch the movie's id from homePage.js (mediaApiId)
     * ** Call TMDB to get the details of the movie to set up movieDisplay.html
     * 
     * ** User and Boards are use for adding media into theme board logic.
     * ** All the boards created by the current logged in user will be fetched every
     * user clicked in any movie.
     * ** This way, their boards will be displayed in the drop down box in the page.
     * ** => Allow users to add media into their created theme-boards.
     */
    @GetMapping("/{id}")
    public String getMovie(@PathVariable Long id,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {

        try {
            // Get the movie (1 object)
            MovieResponse movie = movieService.getMovieWithProviders(id);

            // Identify the User in order to get all their boards
            User user = currentAccountService.getCurrentAccount(authentication);
            // Use user to find all the boards that this user created
            List<Boards> boards = boardsService.findBoardsByUser(user);

            model.addAttribute("movie", movie);
            model.addAttribute("boards", boards);

            return "boardsLayout/mediaDisplay/movieDisplay";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unable to load this movie. Please try again later.");
            return "redirect:/";
        }
    }

}
