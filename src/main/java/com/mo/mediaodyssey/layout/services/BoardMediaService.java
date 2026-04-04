package com.mo.mediaodyssey.layout.services;

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

    public void addMediaToBoard(Long boardId, Long mediaApiId) {

        // Ensures no duplicate media is in the board chosen
        if (boardMediaRepository.existsByBoardIdAndMediaApiId(boardId, mediaApiId)) {
            throw new RuntimeException("Movie already exists in this board.");
        }

        Boards board = boardsRepository.findById(boardId)
                    .orElseThrow(() -> new RuntimeException("Board is delete or error occur" ));

        BoardMedia boardMedia = new BoardMedia(); 
            boardMedia.setMediaApiId(mediaApiId);
            boardMedia.setBoard(board);

        boardMediaRepository.save(boardMedia);
    }
}
