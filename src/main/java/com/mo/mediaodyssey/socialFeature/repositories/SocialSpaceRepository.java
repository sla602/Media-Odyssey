package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.SocialSpace;
import com.mo.mediaodyssey.socialFeature.models.SocialSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface SocialSpaceRepository extends JpaRepository<SocialSpace,Integer> {



    List<SocialSpace> findByName(String name);

    boolean existsByName(String name);


}
