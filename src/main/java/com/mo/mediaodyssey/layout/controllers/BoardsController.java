package com.mo.mediaodyssey.layout.controllers;

import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO;
import com.mo.mediaodyssey.socialFeature.services.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.mo.mediaodyssey.layout.DTO.MoviesTMDB.MovieResponse;
import com.mo.mediaodyssey.layout.models.BoardMedia;
import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.repositories.BoardMediaRepository;
import com.mo.mediaodyssey.layout.services.BoardsService;
import com.mo.mediaodyssey.layout.services.MediaServices.MovieService;
import com.mo.mediaodyssey.shared.model.User;

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
    private final PostService postService;
    private final BoardRoleRepository boardRoleRepository;

    public BoardsController (BoardsService boardsService, BoardMediaRepository boardMediaRepository, MovieService movieService, PostService postService, BoardRoleRepository boardRoleRepository) {
        this.boardsService = boardsService; 
        this.boardMediaRepository = boardMediaRepository;
        this.movieService = movieService;
        this.postService = postService;
        this.boardRoleRepository = boardRoleRepository;
    }


    /* Bring user to the page to create a board */
    @GetMapping("/create")
    public String createBoardPage(Model model) {
        model.addAttribute("board", new Boards());

        return "boardsLayout/themeBoard/createBoard"; 
    }

    /* After user finished created a board, they should be brought back to the homePage 
    *
    ** Notice: the return is set to redirect for the page to automatically reload, in order for the newly
        created boards to appear. If return is setted to actual path of returning to homePage.html then
        no boards will show and error will happen.
    */
    @PostMapping("/create")
    public String createBoard (@ModelAttribute("board") Boards board, Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        board.setUser(user);
        boardsService.createBoard(
                user,
                board.getBoard_name(),
                board.getBoard_description(),
                board.getBoard_type()
        );
        
        return "redirect:/";
    }

    /* Theme boards that are displayed on the homePage are clickable. 
    After clicking on those boards, users will be able to see the details of that boards. 
    Which would be showing the media, description, post, .... */
    @GetMapping("/display/{id}")
    public String navToboardDisplay(@PathVariable("id") Long id, 
                                    Model model, RedirectAttributes redirectAtrrAttributes,
                                    Authentication authentication) {

        // Find whether the board exists or not. 
        Optional<Boards> boardOpt = boardsService.findBoardById(id);

        // If board exists
        if (boardOpt.isPresent()) {
            Boards board = boardOpt.get(); //store board's data
            List<PostDTO> posts = postService.getPostsByBoardId(id);

            User user = (User) authentication.getPrincipal();
            Long currentUserId = user.getId();

            // Get the viewer's role in this board (NONE if not a member)
            String currentUserRole = boardRoleRepository.findByUserIdAndBoardId(currentUserId, id)
                    .map(role -> role.getRoleType().name())
                    .orElse("NONE");

            model.addAttribute("board", board);
            model.addAttribute("posts", posts);
            model.addAttribute("currentUserId", currentUserId);
            model.addAttribute("currentUserRole", currentUserRole);



            return "boardsLayout/themeBoard/boardDisplay";
        } else {
            redirectAtrrAttributes.addFlashAttribute("errorMessage", "Board not found or have been deleted.");
            return "redirect:/";
        }
    }

    /**
     * Returns recent posts for a board as JSON.
     * Called by the frontend when the user clicks the POST button
     * on boardDisplay.html.
     */
    @GetMapping("/display/{boardId}/posts")
    @ResponseBody
    public List<PostDTO> getBoardPosts(@PathVariable Long boardId) {
        return postService.getPostsByBoardId(boardId);
    }


    /**
     * Redirects back to the board page after creation of Post.
     */
    @PostMapping("/display/{boardId}/posts")
    public String createPost(@PathVariable Long boardId,
                             @RequestParam String title,
                             @RequestParam String content,
                             Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        postService.createPost(user.getId(),boardId, title, content);

        return "redirect:/boards/display/" + boardId;
    }

    @PostMapping("/display/{boardId}/posts/{postId}/delete")
    public String deletePost(@PathVariable Long boardId,
                             @PathVariable Long postId,
                             Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        postService.deletePost(user.getId(), postId);

        return "redirect:/boards/display/" + boardId;
    }

    /* Display all MOVIE MEDIAS that are put in the boards by users:
    * ** Logic: get the board_id from browser path -> find all BoardMedia Object 
    * ** -> BoardMedia stores mediaApiId -> get all mediaApiIds -> Call TMDB and find movies by mediaApiIds
    * ** Returns the info to boardDisplay.html so front-end can work with displaying movies as cards.
    * 
    * ** Error Message: This function is actived when user clicked on a board that is already existed.
    * ** If backend cannot find a board by Id when user is able to click on it. Something is wrong with application.
    */
   @GetMapping("/display/{boardId}/movies")
   @ResponseBody
   public List<MovieResponse> getBoardMovies (@PathVariable Long boardId) {
        
        List<BoardMedia> boardMediaList = boardMediaRepository.findByBoardId(boardId);
        List<Long> mediaApiIds = boardMediaList.stream().map(BoardMedia::getMediaApiId).toList();

        return movieService.getMoviesByIds(mediaApiIds);
   }
   
    
}
