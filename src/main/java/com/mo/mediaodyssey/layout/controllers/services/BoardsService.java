package com.mo.mediaodyssey.layout.controllers.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mo.mediaodyssey.layout.controllers.models.Boards;
import com.mo.mediaodyssey.repository.BoardsRepository;

@Service
public class BoardsService {
    
    private final BoardsRepository boardsRepository; 

    public BoardsService (BoardsRepository boardsRepository) {
        this.boardsRepository = boardsRepository; 
    }

    public void createBoard(Boards board) {
        boardsRepository.save(board); 
    }

    public List<Boards> findAllBoards () {
        return boardsRepository.findAll();
    }
}
