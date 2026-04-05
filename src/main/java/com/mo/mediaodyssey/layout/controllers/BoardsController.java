package com.mo.mediaodyssey.layout.controllers;

import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.socialFeature.enums.RoleType;
import com.mo.mediaodyssey.socialFeature.models.Comment;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommentDTO;
import com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO;
import com.mo.mediaodyssey.socialFeature.models.Post;
import com.mo.mediaodyssey.socialFeature.models.Report;
import com.mo.mediaodyssey.socialFeature.repositories.CommentRepository;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import com.mo.mediaodyssey.socialFeature.repositories.ReportRepository;
import com.mo.mediaodyssey.socialFeature.services.CommentService;
import com.mo.mediaodyssey.socialFeature.services.ModerationService;
import com.mo.mediaodyssey.socialFeature.services.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.layout.DTO.MoviesTMDB.MovieResponse;
import com.mo.mediaodyssey.layout.models.BoardMedia;
import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.repositories.BoardMediaRepository;
import com.mo.mediaodyssey.layout.services.BoardsService;
import com.mo.mediaodyssey.layout.services.MediaServices.MovieService;

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
    private final CommentService commentService;
    private final ReportRepository reportRepository;
    private final ModerationService moderationService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public BoardsController (BoardsService boardsService, BoardMediaRepository boardMediaRepository, MovieService movieService, PostService postService, BoardRoleRepository boardRoleRepository, CommentService commentService, ReportRepository reportRepository, ModerationService moderationService, PostRepository postRepository, CommentRepository commentRepository) {
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
    }


    // ─── Helper ──────────────────────────────────────────────────────

    private String getUserRole(Long userId, Long boardId) {
        return boardRoleRepository.findByUserIdAndBoardId(userId, boardId)
                .map(role -> role.getRoleType().name())
                .orElse("NONE");
    }

    private RoleType getUserRoleType(Long userId, Long boardId) {
        return boardRoleRepository.findByUserIdAndBoardId(userId, boardId)
                .map(BoardRole::getRoleType)
                .orElse(RoleType.NONE);
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
        switch (view) {
            case "post":
                if (postId != null) {
                    Post post = postService.getPostById(postId);
                    if (post != null) {
                        List<CommentDTO> comments = commentService.getCommentsWithDepth(postId);
                        model.addAttribute("selectedPost", post);
                        model.addAttribute("comments", comments);
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
                        break;
                    case "ownership":
                        model.addAttribute("members", moderationService.getBoardMembers(id));
                        break;
                    default:
                        List<Report> reports = moderationService.getUnresolvedReports(id);
                        Map<Long, String> postTitles = new HashMap<>();
                        Map<Long, String> postContents = new HashMap<>();
                        Map<Long, String> commentContents = new HashMap<>();

                        for (Report report : reports) {

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
                break;
        }

        return "boardsLayout/themeBoard/boardDisplay";
    }




    // ─── Join / Leave ────────────────────────────────────────────────

    @PostMapping("/display/{boardId}/join")
    public String joinBoard(@PathVariable Long boardId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        boardsService.joinBoard(user.getId(), boardId);
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

    //createPost
    @PostMapping("/display/{boardId}/posts/{postId}/edit")
    public String editPost(@PathVariable Long boardId,
                           @PathVariable Long postId,
                           @RequestParam String title,
                           @RequestParam String content,
                           Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        postService.updatePost(postId, title, content);

        // Redirect back to the post list (default view)
        return "redirect:/boards/display/" + boardId;
    }

    //delete post
    @PostMapping("/display/{boardId}/posts/{postId}/delete")
    public String deletePost(@PathVariable Long boardId,
                             @PathVariable Long postId,
                             Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        postService.deletePost(user.getId(), postId);

        return "redirect:/boards/display/" + boardId;
    }

    //creating comment to comment (no parent)
    @PostMapping("/display/{boardId}/posts/{postId}/comments")
    public String createComment(@PathVariable Long boardId,
                                @PathVariable Long postId,
                                @RequestParam String content,
                                Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        commentService.createComment(user.getId(), postId, content);

        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }


//edit comment
    @PostMapping("/display/{boardId}/posts/{postId}/comments/{commentId}/edit")
    public String editComment(@PathVariable Long boardId,
                              @PathVariable Long postId,
                              @PathVariable Long commentId,
                              @RequestParam String content,
                              Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        commentService.updateCommentContent(commentId, content);

        // Stay on the single post + comments view
        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }

    //replying to comment (to parent comment)
    @PostMapping("/display/{boardId}/posts/{postId}/comments/{commentId}/reply")
    public String replyToComment(@PathVariable Long boardId,
                                 @PathVariable Long postId,
                                 @PathVariable Long commentId,
                                 @RequestParam String content,
                                 Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        commentService.replyToComment(user.getId(), commentId, content);

        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }




    //deleting comment
    @PostMapping("/display/{boardId}/posts/{postId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long boardId,
                                @PathVariable Long postId,
                                @PathVariable Long commentId) {

        commentService.softDeleteComment(commentId);

        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }


    // ─── Reporting ───────────────────────────────────────────────────

    @PostMapping("/display/{boardId}/posts/{postId}/report")
    public String reportPost(@PathVariable Long boardId,
                             @PathVariable Long postId,
                             @RequestParam String reason,
                             @RequestParam Long contentAuthorId,
                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        moderationService.reportPost(boardId, postId, user.getId(), contentAuthorId, reason);
        return "redirect:/boards/display/" + boardId;
    }

    @PostMapping("/display/{boardId}/posts/{postId}/comments/{commentId}/report")
    public String reportComment(@PathVariable Long boardId,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                @RequestParam String reason,
                                @RequestParam Long contentAuthorId,
                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        moderationService.reportComment(boardId, commentId, user.getId(), contentAuthorId, reason);
        return "redirect:/boards/display/" + boardId + "?view=post&postId=" + postId;
    }

    // ─── Moderation Actions ──────────────────────────────────────────

    @PostMapping("/display/{boardId}/moderation/reports/{reportId}/dismiss")
    public String dismissReport(@PathVariable Long boardId, @PathVariable Long reportId) {
        moderationService.dismissReport(reportId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=reports";
    }

    @PostMapping("/display/{boardId}/moderation/reports/{reportId}/delete-content")
    public String deleteReportedContent(@PathVariable Long boardId, @PathVariable Long reportId) {
        moderationService.deleteReportedContent(reportId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=reports";
    }

    @PostMapping("/display/{boardId}/moderation/reports/{reportId}/ban")
    public String banFromReport(@PathVariable Long boardId, @PathVariable Long reportId) {
        moderationService.banFromReport(reportId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=reports";
    }

    @PostMapping("/display/{boardId}/moderation/members/{userId}/unban")
    public String unbanMember(@PathVariable Long boardId, @PathVariable Long userId) {
        moderationService.unbanMember(userId, boardId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=members";
    }

    @PostMapping("/display/{boardId}/moderation/members/{userId}/ban")
    public String banMember(@PathVariable Long boardId, @PathVariable Long userId) {
        moderationService.banMember(userId, boardId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=members";
    }

    @PostMapping("/display/{boardId}/moderation/members/{userId}/promote")
    public String promoteMember(@PathVariable Long boardId, @PathVariable Long userId) {
        moderationService.promoteMember(userId, boardId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=members";
    }

    @PostMapping("/display/{boardId}/moderation/members/{userId}/demote")
    public String demoteModerator(@PathVariable Long boardId, @PathVariable Long userId) {
        moderationService.demoteModerator(userId, boardId);
        return "redirect:/boards/display/" + boardId + "?view=moderation&modTab=members";
    }

    // ─── Ownership Actions (inside moderation) ───────────────────────

    @PostMapping("/display/{boardId}/moderation/ownership/transfer")
    public String transferOwnership(@PathVariable Long boardId,
                                    @RequestParam Long newOwnerId,
                                    Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        moderationService.transferOwnership(user.getId(), newOwnerId, boardId);
        return "redirect:/boards/display/" + boardId;
    }

    @PostMapping("/display/{boardId}/moderation/ownership/delete")
    public String hardDeleteBoard(@PathVariable Long boardId,
                                  Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        RoleType role = getUserRoleType(user.getId(), boardId);
        if (!role.isOwner()) {
            throw new SecurityException("Only the owner can delete the board");
        }
        moderationService.hardDeleteBoard(boardId);
        return "redirect:/";
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
   public List<MovieResponse> getBoardMovies (@PathVariable Long boardId) {
        
        List<BoardMedia> boardMediaList = boardMediaRepository.findByBoardId(boardId);
        List<Long> mediaApiIds = boardMediaList.stream().map(BoardMedia::getMediaApiId).toList();

        return movieService.getMoviesByIds(mediaApiIds);
   }
   
    
}
