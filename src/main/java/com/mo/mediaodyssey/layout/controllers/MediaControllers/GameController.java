package com.mo.mediaodyssey.layout.controllers.MediaControllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mo.mediaodyssey.layout.services.MediaServices.GameService;

@RestController
@RequestMapping("/mediaView")
public class GameController {

    @Autowired
    private GameService gameService; 

    public String getGame (@PathVariable Long id,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            String game = gameService.getGameById(id); 
            model.addAttribute("game", game); 
            return "boardsLayout/mediaDisplay/gameDisplay"; 
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to fetch game details. Please try again.");
            return "redirect:/home";
        }   
    }
    
}
