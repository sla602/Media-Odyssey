package com.mo.mediaodyssey.layout.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mo.mediaodyssey.layout.controllers.models.Movies;
import com.mo.mediaodyssey.layout.controllers.services.MoviesService;

@RestController
public class MoviesController {
    
    private final MoviesService moviesService; 

    public MoviesController (MoviesService moviesService) {
        this.moviesService = moviesService; 
    }

    @GetMapping("/search")
    public Movies searchMovie (@RequestParam String title) {
        return moviesService.getMovie(title); 
    }
}
