package com.mo.mediaodyssey.layout.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MappingController {

    /* This Controller is to navigates elements like create board icon, home icon, 
    elements in side bar, and elements view in singular board layout */

    /* HEADER ELEMENTS */
    @GetMapping("/homeReturn")
    public String homeReturn() {
        return "/boardsLayout/homePage";
    }
    
    /* SIDE BAR ELEMENTS */

    @GetMapping("/socialTab")
    public String navToSocialTab() {
        return "social";
    }

    @GetMapping("/trendingTab")
    public String navToTrendingTab() {
        return "trending";
    }

    /* ADMIN BUTTON TO USER */
    @GetMapping("/admin/users")
    public String navToUserDB() {
        return "user"; 
    }
}
