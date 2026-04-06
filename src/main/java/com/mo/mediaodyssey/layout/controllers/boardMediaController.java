package com.mo.mediaodyssey.layout.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mo.mediaodyssey.layout.services.BoardMediaService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/boards")
public class boardMediaController {
    
    @Autowired
    private BoardMediaService boardMediaService;

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
    public ResponseEntity<?> addMediaToBoard(@PathVariable Long board_id, 
                                            @RequestBody Map<String, Object> body) {
        try {
            Long mediaApiId = null;
            String mediaType = (String) body.get("type");

            // For movies/games
            if (body.containsKey("mediaApiId")) {
                mediaApiId = Long.valueOf(body.get("mediaApiId").toString());
            }

            // For music
            if (body.containsKey("artist") && body.containsKey("track")) {
                mediaType = "music";
            }

            boardMediaService.addMediaToBoard(board_id, mediaApiId, mediaType, body);

            return ResponseEntity.ok(Map.of("status", "success", "message", "Media added to board"));
        } catch (RuntimeException e) {
            // Return a 400 with the error message
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            // Catch all unexpected errors
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Server error"));
        }
    }
    
}
