package com.mo.mediaodyssey.layout.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class MappingController {

    /* This Controller is to navigates elements like create board icon, 
    elements in side bar, and elements view in singular board layout */

    /* SIDE BAR ELEMENTS */

    /* Bring user to the page specifically for social feature.
    Social feature needs to be discussed more specifically tho.*/
    @GetMapping("/socialTab")
    public String navToSocialTab() {
        return "boardsLayout/features/social";
    }
    
    /* Navigates the user to explore other boards.*/
    @GetMapping("/exploreBoard")
    public String navToExplore() {
        return "boardsLayout/themeBoard/exploreBoards";
    }
    
    /* Bring user to the page specifically for trending feature */
    /**
     * Redirect old trending route to the real community controller route.
     *
     * This fixes the earlier issue where opening /trendingTab directly
     * bypassed the controller logic and therefore broke filtering/sorting.
    */
    @GetMapping("/trendingTab")
    public String navToTrendingTab() {
        return "redirect:/community";
    }

    @GetMapping("/userProfile")
    public String navToUserProfile() {
        return "redirect:/profile";
    }

    /* Bring user to the Liked Media page */
    @GetMapping("/likedMediaTab")
    public String navToLikedMediaTab() {
        return "boardsLayout/features/likedMedia";
    }
    
}