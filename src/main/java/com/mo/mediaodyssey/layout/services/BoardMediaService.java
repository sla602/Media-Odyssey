package com.mo.mediaodyssey.layout.services;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mo.mediaodyssey.layout.models.BoardMedia;
import com.mo.mediaodyssey.layout.models.Boards;
import com.mo.mediaodyssey.layout.repositories.BoardMediaRepository;
import com.mo.mediaodyssey.layout.repositories.BoardsRepository;

@Service
public class BoardMediaService {

    @Autowired
    private BoardMediaRepository boardMediaRepository; 

    @Autowired
    private BoardsRepository boardsRepository; 

    public void addMediaToBoard(Long boardId, Long mediaApiId, String mediaType, Map<String, Object> body) {

        Boards board = boardsRepository.findById(boardId)
                    .orElseThrow(() -> new RuntimeException("Board is deleted or error occurred"));

        BoardMedia boardMedia = new BoardMedia();
        boardMedia.setBoard(board);

        if ("music".equals(mediaType)) {
            String artist = body.get("artist").toString();
            String track = body.get("track").toString();

            if (boardMediaRepository.existsByBoardIdAndMediaTypeAndArtistAndTrack(boardId, mediaType, artist, track)) {
                throw new RuntimeException("This song already exists in this board.");
            }

            boardMedia.setMediaType("music");
            boardMedia.setArtist(artist);
            boardMedia.setTrack(track);

        } else {
            if (boardMediaRepository.existsByBoardIdAndMediaTypeAndMediaApiId(boardId, mediaType, mediaApiId)) {
                throw new RuntimeException("This media already exists in this board.");
            }

            boardMedia.setMediaType(mediaType); // "movie" or "game"
            boardMedia.setMediaApiId(mediaApiId);
        }

        boardMediaRepository.save(boardMedia);
    }

    /*
    public void addMediaToBoard(Long boardId, Map<String, Object> body) {

        String mediaType = (String) body.get("type");

        Boards board = boardsRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));

        BoardMedia boardMedia = new BoardMedia();
        boardMedia.setBoard(board);
        boardMedia.setMediaType(mediaType);

        if (mediaType.equals("movie") || mediaType.equals("game")) {

            Long mediaApiId = Long.valueOf(body.get("mediaApiId").toString());

            if (boardMediaRepository.existsByBoardIdAndMediaTypeAndMediaApiId(boardId, mediaType, mediaApiId)) {
                throw new RuntimeException("You already have this in this board.");
            }

            boardMedia.setMediaApiId(mediaApiId);

        } else if (mediaType.equals("music")) {

            String artist = (String) body.get("artist");
            String track = (String) body.get("track");

            if (boardMediaRepository.existsByBoardIdAndMediaTypeAndArtistAndTrack(boardId, mediaType, artist, track)) {
                throw new RuntimeException("You already have this in this board.");
            }

            boardMedia.setMediaApiId(null);
            boardMedia.setArtist(artist);
            boardMedia.setTrack(track);
        }

        boardMediaRepository.save(boardMedia);
    } */
}
