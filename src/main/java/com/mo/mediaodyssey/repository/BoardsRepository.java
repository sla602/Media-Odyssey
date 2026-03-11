package com.mo.mediaodyssey.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mo.mediaodyssey.layout.controllers.models.Boards;

@Repository
public interface BoardsRepository extends JpaRepository<Boards, Long> {

}
