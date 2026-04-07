package com.mo.mediaodyssey.layout.controllers;

import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.socialFeature.models.BoardInvite;
import com.mo.mediaodyssey.socialFeature.repositories.ProfileRepository;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommentDTO;
import com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.socialFeature.models.Report;
import com.mo.mediaodyssey.socialFeature.repositories.CommentRepository;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import com.mo.mediaodyssey.socialFeature.repositories.ReportRepository;
import com.mo.mediaodyssey.socialFeature.services.*;
import com.mo.mediaodyssey.layout.services.MediaServices.MusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.util.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.layout.DTO.BoardMediaDTO;
import com.mo.mediaodyssey.layout.DTO.GamesRAWG.GameResponse;
import com.mo.mediaodyssey.layout.DTO.MoviesTMDB.MovieResponse;
import com.mo.mediaodyssey.layout.models.BoardMedia;
import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.models.MediaModels.Music;
import com.mo.mediaodyssey.layout.repositories.BoardMediaRepository;
import com.mo.mediaodyssey.layout.services.BoardsService;
import com.mo.mediaodyssey.layout.services.MediaServices.GameService;
import com.mo.mediaodyssey.layout.services.MediaServices.MovieService;

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
    private final CommentService commentService;
    private final ReportRepository reportRepository;
    private final ModerationService moderationService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final BoardInviteService boardInviteService;

    @Autowired
    private GameService gameService; 
    @Autowired
    private MusicService musicService;

    public BoardsController (BoardsService boardsService, BoardMediaRepository boardMediaRepository, MovieService movieService, PostService postService, BoardRoleRepository boardRoleRepository, CommentService commentService, ReportRepository reportRepository, ModerationService moderationService, PostRepository postRepository, CommentRepository commentRepository,
                             ProfileRepository profileRepository, ProfileService profileService, BoardInviteService boardInviteService) {
        this.boardsService = boardsService; 
        this.boardMediaRepository = boardMediaRepository;
        this.movieService = movieService;
        this.postService = postService;
        this.boardRoleRepository = boardRoleRepository;
        this.commentService = commentService;
        this.reportRepository = reportRepository;
        this.moderationService = moderationService;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.profileRepository = profileRepository;
        this.profileService = profileService;
        this.boardInviteService = boardInviteService;
    }


    // ─── Helper ──────────────────────────────────────────────────────

    private String getUserRole(Long userId, Long boardId) {
        return boardRoleRepository.findByUserIdAndBoardId(userId, boardId)
                .map(role -> role.getRoleType().name())
                .orElse("NONE");
    }

    /**
     * Build a map of userId -> display name for the given set of IDs.
     * Uses the profile's username if set; otherwise falls back to "User #{id}".
     * Safe to call with nulls in the collection — they're filtered out.
     */
    private Map<Long, String> buildUserDisplayNames(Collection<Long> userIds) {
        Map<Long, String> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return result;

        Set<Long> distinctIds = new HashSet<>();
        for (Long id : userIds) {
            if (id != null) distinctIds.add(id);
        }
        if (distinctIds.isEmpty()) return result;

        // Seed every ID with the fallback first, then overwrite with username if present.
        for (Long id : distinctIds) {
            result.put(id, "User #" + id);
        }
        for (Long id : distinctIds) {
            profileRepository.findByUserId(id).ifPresent(profile -> {
                String name = profile.getUsername();
                if (name != null && !name.isBlank()) {
                    result.put(id, name);
                }
            });
        }
        return result;
    }

    /* Bring user to the page to create a board */
    @GetMapping("/create")
    public String createBoardPage(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();

        // Gate: username required before creating a board
        if (!profileService.hasUsername(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You need to set a username before creating a board.");
            return "redirect:/profile";
        }
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
                                    @RequestParam(value = "view", defaultValue = "posts") String view,
                                    @RequestParam(value = "postId", required = false) Long postId,
                                    @RequestParam(value = "modTab", defaultValue = "reports") String modTab,
                                    @RequestParam(value = "search", required = false) String search,
                                                                Model model,
                                                                RedirectAttributes redirectAttributes,
                                                                Authentication authentication) {

        Optional<Boards> boardOpt = boardsService.findBoardById(id);

        if (boardOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Board not found or has been deleted.");
            return "redirect:/";
        }


        Boards board = boardOpt.get();
        User user = (User) authentication.getPrincipal();

        //check if banned user
        String role = getUserRole(user.getId(), id);

        if ("BANNED".equals(role)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are banned from this board.");
            return "redirect:/";
        }

        boolean isMember = !"NONE".equals(role) && !"LEFT".equals(role) && !"BANNED".equals(role);


        // ADDED: Private-board gate. Non-members of a private board get a
// "this is private, you need an invitation" page instead of the board.
        if ("private".equalsIgnoreCase(board.getBoard_type()) && !isMember) {
            model.addAttribute("board", board);
            model.addAttribute("currentUserId", user.getId());
            return "boardsLayout/themeBoard/privateBoardGate";
        }

        Long reportCount = reportRepository.countByBoardIdAndResolvedFalse(id);

        model.addAttribute("board", board);
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("currentUserRole", role);
        model.addAttribute("isMember", isMember);
        model.addAttribute("reportCount", reportCount);
        model.addAttribute("currentView", view);
        model.addAttribute("modTab", modTab);


        // Defaults
        model.addAttribute("selectedPost", null);
        model.addAttribute("comments", Collections.emptyList());
        model.addAttribute("posts", Collections.emptyList());
        model.addAttribute("reports", Collections.emptyList());
        model.addAttribute("members", Collections.emptyList());

        Set<Long> userIdsToResolve = new HashSet<>();

        switch (view) {
            case "post":
                if (postId != null) {
                    Post post = postService.getPostById(postId);
                    if (post != null) {
                        List<CommentDTO> comments = commentService.getCommentsWithDepth(postId);
                        model.addAttribute("selectedPost", post);
                        model.addAttribute("comments", comments);
                        userIdsToResolve.add(post.getAuthorId());

                        for (CommentDTO comment : comments) {
                            userIdsToResolve.add(comment.getAuthorId());
                        }
                    }
                }
                break;

            case "moderation":
                switch (modTab) {
                    case "members":
                        List<BoardRole> members;
                        if (search != null && !search.isBlank()) {
                            members = moderationService.searchBoardMembers(id, search);
                        } else {
                            members = moderationService.getBoardMembers(id);
                        }
                        model.addAttribute("members", members);
                        model.addAttribute("search", search);
                        for (BoardRole member : members) {
                            userIdsToResolve.add(member.getUserId());
                        }
                        break;
                    case "ownership":

                        List<BoardRole> ownershipMembers = moderationService.getBoardMembers(id);
                        model.addAttribute("members", ownershipMembers);

                        for (BoardRole member : ownershipMembers) {
                            userIdsToResolve.add(member.getUserId());
                        }
                        break;
                    case "invites":
                        // Only meaningful for private boards; the tab link is hidden
                        // otherwise, but guard here too for direct URL hits.
                        // Loads invitable friends + pending invites in one place
                        // (no more duplicate blocks scattered around the method).

                            List<User> invitable =
                                    boardInviteService.getInvitableFriends(user.getId(), id);
                            List<BoardInvite> pending =
                                    boardInviteService.getInvitesForBoard(id);

                            model.addAttribute("invitableFriends", invitable);
                            model.addAttribute("pendingInvites", pending);

                            // Feed ids into userIdsToResolve so buildUserDisplayNames
                            // at the bottom of this method populates userDisplayNames
                            // for both invitable friends and pending invite rows.
                            for (User f : invitable) userIdsToResolve.add(f.getId());
                            for (BoardInvite inv : pending) {
                                userIdsToResolve.add(inv.getInviteeUserId());
                                userIdsToResolve.add(inv.getInviterUserId());
                        }
                        break;
                    default:
                        List<Report> reports = moderationService.getUnresolvedReports(id);
                        Map<Long, String> postTitles = new HashMap<>();
                        Map<Long, String> postContents = new HashMap<>();
                        Map<Long, String> commentContents = new HashMap<>();

                        for (Report report : reports) {
                            userIdsToResolve.add(report.getReportedByUserId());
                            userIdsToResolve.add(report.getContentAuthorId());
                            // Fetch post data
                            if (report.getPostId() != null) {
                                postService.getPostById(report.getPostId()); // you already have this service

                                postRepository.findById(report.getPostId()).ifPresent(post -> {
                                    postTitles.put(report.getId(), post.getTitle());
                                    postContents.put(report.getId(), post.getContent());
                                });
                            }

                            // Fetch comment data
                            if (report.getCommentId() != null) {
                                commentRepository.findById(report.getCommentId()).ifPresent(comment -> {
                                    commentContents.put(report.getId(), comment.getContent());
                                    userIdsToResolve.add(comment.getAuthorId());
                                });
                            }
                        }

                        model.addAttribute("reports", reports);
                        model.addAttribute("postTitles", postTitles);
                        model.addAttribute("postContents", postContents);
                        model.addAttribute("commentContents", commentContents);
                        break;
                }
                break;

            case "settings":
                break;

            default:
                List<PostDTO> posts = postService.getPostsByBoardId(id);
                model.addAttribute("posts", posts);

                for (PostDTO post : posts) {
                    userIdsToResolve.add(post.getAuthorId());
                }
                break;
        }


        model.addAttribute("userDisplayNames", buildUserDisplayNames(userIdsToResolve));

        return "boardsLayout/themeBoard/boardDisplay";
    }


    // ─── Join / Leave ────────────────────────────────────────────────

    @PostMapping("/display/{boardId}/join")
    public String joinBoard(@PathVariable Long boardId, Authentication authentication,RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        if (!profileService.hasUsername(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You need to set a username before joining boards.");
            return "redirect:/profile";
        }

        try {
            boardsService.joinBoard(user.getId(), boardId);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/boards/display/" + boardId;
    }

    @PostMapping("/display/{boardId}/leave")
    public String leaveBoard(@PathVariable Long boardId, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        try {
            boardsService.leaveBoard(user.getId(), boardId);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/boards/display/" + boardId;
    }


    // ─── Board Settings ──────────────────────────────────────────────

    @PostMapping("/display/{boardId}/settings/edit")
    public String editBoard(@PathVariable Long boardId,
                            @RequestParam String boardName,
                            @RequestParam String boardDescription,
                            @RequestParam String boardType,
                            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        boardsService.editBoard(user.getId(), boardId, boardName, boardDescription, boardType);
        return "redirect:/boards/display/" + boardId + "?view=settings";
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
    public List<BoardMediaDTO> getBoardMovies(@PathVariable Long boardId) {

        List<BoardMedia> list = boardMediaRepository.findByBoardId(boardId);
        List<BoardMediaDTO> result = new ArrayList<>();

        for (BoardMedia m : list) {
            if (!"movie".equals(m.getMediaType())) continue;

            MovieResponse movie = movieService.getMovieById(m.getMediaApiId());
            if (movie == null) continue;

            BoardMediaDTO dto = new BoardMediaDTO();
            dto.setId(m.getId()); 
            dto.setMediaApiId(m.getMediaApiId());
            dto.setMediaType("movie");

            dto.setTitle(movie.getTitle());
            dto.setPoster_path(movie.getPoster_path());

            result.add(dto);
        }

        return result;
    }

    //For games 
    @GetMapping("/display/{boardId}/games")
    @ResponseBody
    public List<BoardMediaDTO> getBoardGames(@PathVariable Long boardId) {

        List<BoardMedia> list = boardMediaRepository.findByBoardId(boardId);
        List<BoardMediaDTO> result = new ArrayList<>();

        for (BoardMedia m : list) {
            if (!"game".equals(m.getMediaType())) continue;

            GameResponse game = gameService.getGameById(m.getMediaApiId());
            if (game == null) continue; // ✅ safety

            BoardMediaDTO dto = new BoardMediaDTO();
            dto.setId(m.getId());
            dto.setMediaApiId(m.getMediaApiId());
            dto.setMediaType("game");

            dto.setTitle(game.getTitle());

            dto.setPoster_path(game.getPoster_path());

            result.add(dto);
        }

        return result;
    }

    // For Music
    @GetMapping("/display/{boardId}/music")
    @ResponseBody
    public List<BoardMediaDTO> getBoardMusic(@PathVariable Long boardId) {

        List<BoardMedia> list = boardMediaRepository.findByBoardId(boardId);
        List<BoardMediaDTO> result = new ArrayList<>();

        for (BoardMedia m : list) {
            if (!"music".equals(m.getMediaType())) continue;

            BoardMediaDTO dto = new BoardMediaDTO();
            dto.setId(m.getId()); 
            dto.setMediaType("music");

            dto.setTitle(m.getTrack());
            dto.setArtist(m.getArtist());

            // convert from Last.fm API response
            Music musicInfo = musicService.convertToMusic(m.getArtist(), m.getTrack()); 
            String poster = null;

            if (musicInfo != null && musicInfo.getPoster_path() != null) {
            poster = musicInfo.getPoster_path();
            }

            dto.setPoster_path(poster != null ? poster : "Could not get image of this song :(");
            System.out.println("Music DTO poster: " + dto.getPoster_path());

            result.add(dto);
        }
        return result;
    }
}