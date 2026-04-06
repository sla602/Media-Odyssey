package com.mo.mediaodyssey.layout.controllers.MediaControllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.models.MediaModels.Music;
import com.mo.mediaodyssey.layout.services.BoardsService;
import com.mo.mediaodyssey.layout.services.MediaServices.MusicService;
import com.mo.mediaodyssey.shared.model.User;

@Controller
@RequestMapping("/mediaView/song")
public class MusicController {

    @Autowired
    private MusicService musicService;
    
    @Autowired
    private BoardsService boardsService; 

    // Last FM is weird, id is not efficient, take artist and track (song name)
    // The homePage.js is also adjust for this unique path
    @GetMapping("/{artist}/{track}")
    public String getSong (@PathVariable String artist, @PathVariable String track,
                                Model model, RedirectAttributes redirectAttributes, Authentication authentication)
    {
        try {
            Music music = musicService.convertToMusic(artist, track);

            User user = (User) authentication.getPrincipal();
            List<Boards> boards = boardsService.findBoardsByUser(user); 

            model.addAttribute("music", music);
            model.addAttribute("boards", boards); 

            return "boardsLayout/mediaDisplay/musicDisplay"; 

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", 
            "Unable to load this song. Please try again later.");
            return "redirect:/";
        }
    }
}
