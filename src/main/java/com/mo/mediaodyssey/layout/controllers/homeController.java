package com.mo.mediaodyssey.layout.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.mo.mediaodyssey.layout.controllers.services.BoardsService;

@Controller
public class homeController {

    /* MAP THE INITIAL LOCALHOST:8080 DIRECTLY TO HOMEPAGE (WILL NEED REDIRECT) */
    
    private final BoardsService boardsService;

    public homeController (BoardsService boardsService) {
        this.boardsService = boardsService;
    }
    
    @GetMapping("/")
    public String home(Model model){
        model.addAttribute("boards", boardsService.findAllBoards());

        return "/boardsLayout/homePage";
    }
}
