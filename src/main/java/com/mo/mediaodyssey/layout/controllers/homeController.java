package com.mo.mediaodyssey.layout.controllers;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.services.BoardsService;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

@Controller
public class homeController {

    /* After users logged in, they will be direct to homePage.html */

    private final BoardsService boardsService;
    private final CurrentAccountService currentAccountService;

    public homeController(BoardsService boardsService, CurrentAccountService currentAccountService) {
        this.boardsService = boardsService;
        this.currentAccountService = currentAccountService;
    }

    /*
     * homePage Mapping:
     *** In the case of not finding any boards (this will always happen for new
     * users):
     ** homePage.html will not displayed any theme boards in
     * "Jounreys you have joined".
     *
     * * Logic: This application does not include any unique username, it focuses on
     * email.
     * Therefore, authentication.getName() will return user's email.
     */
    @GetMapping("/")
    public String home(Model model, Authentication authentication) {

        User user = currentAccountService.getCurrentAccount(authentication);

        // shows only created boards by the user
        // List<Boards> boards = boardsService.findBoardsByUser(user);

        // all roltype joined boards
        List<Boards> boards = boardsService.getJoinedBoards(user.getId());

        model.addAttribute("boards", boards);

        // Keep the verification reminder on the destination page so a fast
        // login redirect does not hide it.
        if (!user.isEmailVerified()) {
            model.addAttribute("warningMessage",
                    "Your email is not verified yet. Please verify it when possible.");
        }

        return "boardsLayout/homePage";
    }
}
