package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.Community;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CommunityRepository extends JpaRepository<Community,Integer> {



    List<Community> findByName(String name);

    boolean existsByName(String name);



}
