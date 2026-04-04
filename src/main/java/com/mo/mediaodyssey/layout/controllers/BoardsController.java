package com.mo.mediaodyssey.layout.controllers;

import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.mo.mediaodyssey.layout.DTO.MoviesTMDB.MovieResponse;
import com.mo.mediaodyssey.layout.models.BoardMedia;
import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.repositories.BoardMediaRepository;
import com.mo.mediaodyssey.layout.services.BoardsService;
import com.mo.mediaodyssey.layout.services.MediaServices.MovieService;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/boards")
public class BoardsController {
    /* boards controller is a mapping controller for board related htmls */

    private final BoardsService boardsService;
    private final BoardMediaRepository boardMediaRepository;
    private final MovieService movieService;
    private final CurrentAccountService currentAccountService;

    public BoardsController(BoardsService boardsService, BoardMediaRepository boardMediaRepository,
            MovieService movieService, CurrentAccountService currentAccountService) {
        this.boardsService = boardsService;
        this.boardMediaRepository = boardMediaRepository;
        this.movieService = movieService;
        this.currentAccountService = currentAccountService;
    }

    /* Bring user to the page to create a board */
    @GetMapping("/create")
    public String createBoardPage(Model model) {
        model.addAttribute("board", new Boards());

        return "boardsLayout/themeBoard/createBoard";
    }

    /*
     * After user finished created a board, they should be brought back to the
     * homePage
     *
     ** Notice: the return is set to redirect for the page to automatically reload,
     * in order for the newly
     * created boards to appear. If return is setted to actual path of returning to
     * homePage.html then
     * no boards will show and error will happen.
     */
    @PostMapping("/create")
    public String createBoard(@ModelAttribute("board") Boards board, Authentication authentication) {

        User user = currentAccountService.getCurrentAccount(authentication);

        board.setUser(user);
        boardsService.createBoard(board);

        return "redirect:/";
    }

    /*
     * Theme boards that are displayed on the homePage are clickable.
     * After clicking on those boards, users will be able to see the details of that
     * boards.
     * Which would be showing the media, description, post, ....
     */
    @GetMapping("/display/{id}")
    public String navToboardDisplay(@PathVariable("id") Long id,
            Model model, RedirectAttributes redirectAtrrAttributes,
            Authentication authentication) {

        // Find whether the board exists or not.
        Optional<Boards> boardOpt = boardsService.findBoardById(id);

        // If board exists
        if (boardOpt.isPresent()) {
            Boards board = boardOpt.get(); // store board's data

            model.addAttribute("board", board);
            return "boardsLayout/themeBoard/boardDisplay";
        } else {
            redirectAtrrAttributes.addFlashAttribute("errorMessage", "Board not found or have been deleted.");
            return "redirect:/";
        }
    }

    /*
     * Display all MOVIE MEDIAS that are put in the boards by users:
     * ** Logic: get the board_id from browser path -> find all BoardMedia Object
     * ** -> BoardMedia stores mediaApiId -> get all mediaApiIds -> Call TMDB and
     * find movies by mediaApiIds
     * ** Returns the info to boardDisplay.html so front-end can work with
     * displaying movies as cards.
     * 
     * ** Error Message: This function is actived when user clicked on a board that
     * is already existed.
     * ** If backend cannot find a board by Id when user is able to click on it.
     * Something is wrong with application.
     */
    @GetMapping("/display/{boardId}/movies")
    @ResponseBody
    public List<MovieResponse> getBoardMovies(@PathVariable Long boardId) {

        List<BoardMedia> boardMediaList = boardMediaRepository.findByBoardId(boardId);
        List<Long> mediaApiIds = boardMediaList.stream().map(BoardMedia::getMediaApiId).toList();

        return movieService.getMoviesByIds(mediaApiIds);
    }

}
