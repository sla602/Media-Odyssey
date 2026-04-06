package com.mo.mediaodyssey.layout.controllers.MediaControllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.mo.mediaodyssey.layout.DTO.GamesRAWG.GameResponse;
import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.services.BoardsService;
import com.mo.mediaodyssey.layout.services.MediaServices.GameService;
import com.mo.mediaodyssey.shared.model.User;

@Controller
@RequestMapping("/mediaView/game")
public class GameController {

    @Autowired
    private GameService gameService; 

    @Autowired
    private BoardsService boardsService;

    @GetMapping("/{id}")
    public String getGame (@PathVariable Long id,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {

            // Get the game (1 object)
            GameResponse game = gameService.getGameById(id); 

            User user = (User) authentication.getPrincipal();
            List<Boards> boards = boardsService.findBoardsByUser(user);
            
            model.addAttribute("game", game); 
            model.addAttribute("boards", boards);

            return "boardsLayout/mediaDisplay/gameDisplay"; 
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
            "Unable to load this game. Please try again later.");
            return "redirect:/";
        }  
    } 
    
}
