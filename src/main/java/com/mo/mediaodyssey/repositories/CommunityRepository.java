package com.mo.mediaodyssey.repositories;

import com.mo.mediaodyssey.models.Community;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface CommunityRepository extends JpaRepository<Community,Integer> {



    List<Community> findByName(String name);

    boolean existsByName(String name);

}
