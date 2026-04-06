package com.mo.mediaodyssey.layout.controllers;

import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.services.BoardsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ExploreController {

    private final BoardsService boardsService;

    public ExploreController(BoardsService boardsService) {
        this.boardsService = boardsService;
    }

    @GetMapping("/explore")
    public String exploreBoardsPage(@RequestParam(value = "search", required = false) String search,
                                    Model model) {

        List<Boards> boards = boardsService.findAllBoards().stream()
                .filter(b -> "public".equalsIgnoreCase(b.getBoard_type()))
                .collect(Collectors.toList());

        if (search != null && !search.isBlank()) {
            String query = search.toLowerCase();
            boards = boards.stream()
                    .filter(b -> b.getBoard_name().toLowerCase().contains(query)
                            || (b.getBoard_description() != null
                            && b.getBoard_description().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("boards", boards);
        model.addAttribute("search", search);

        return "boardsLayout/themeBoard/exploreBoards";
    }
}