package com.mo.mediaodyssey.layout.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MappingController {

    /*
     * This Controller is to navigates elements like create board icon, elements in
     * side bar, and elements view in singular board layout
     */

    /*=================================d SIDE BAR ELEMENTS ====================================*/

    /*
     * Navigates the user to explore other boards.
     * 
     * Redirect old exploreBoards route to current explore route.
     */
    @GetMapping("/exploreBoards")
    public String navToExplore() {
        return "redirect:/explore";
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

    /* Bring user to the Liked Media page */
    @GetMapping("/likedMediaTab")
    public String navToLikedMediaTab() {
        return "boardsLayout/features/likedMedia";
    }

    @GetMapping("/userProfile")
    public String navToUserProfile() {
        return "redirect:/profile";
    }

}