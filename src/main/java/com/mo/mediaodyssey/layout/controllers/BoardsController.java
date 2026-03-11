package com.mo.mediaodyssey.layout.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.mo.mediaodyssey.layout.controllers.models.Boards;
import com.mo.mediaodyssey.layout.controllers.services.BoardsService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequestMapping("/boards")
public class BoardsController {

    private final BoardsService boardsService; 

    public BoardsController (BoardsService boardsService) {
        this.boardsService = boardsService; 
    }

    @GetMapping("/create")
    public String createBoardPage(Model model) {
        model.addAttribute("board", new Boards());

        return "createBoard"; 
    }

    @PostMapping("/create")
    public String createBoard (@ModelAttribute("board") Boards board) {

        boardsService.createBoard(board);
        
        return "redirect:/";
    }
}
