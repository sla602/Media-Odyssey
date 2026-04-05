package com.mo.mediaodyssey.layout.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mo.mediaodyssey.layout.services.BoardMediaService;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/boards")
public class boardMediaController {
    
    private final BoardMediaService boardMediaService;

    public boardMediaController (BoardMediaService boardMediaService) {
        this.boardMediaService = boardMediaService;
    }

    /*
    * *** This function is used to Post request of adding media into a board. 
    * ** Notice: we most likely don't want users to duplicate any movie/music/game in their board.
    * ** Therefore, this function will check if the selected media has existed in the chosen board or not.
    * 
    * *** This request will only be sent from {media}Display/html. Which means mediaApiId is already there.
    * ** Auth ensures only logged users can access to boards and movies. 
    * ** Therefore, there is no need to doublecheck mediApiId or user_id. 
    */
    @PostMapping("/{board_id}/media")
    public ResponseEntity<?> addMediaToBoard( @PathVariable Long board_id, 
                                            @RequestBody Map<String, Long> body){

        // Get mediaApiId 
        Long mediaApiId = body.get("mediaApiId");

        boardMediaService.addMediaToBoard(board_id, mediaApiId);

        return ResponseEntity.ok().build();
    } 
    
}
