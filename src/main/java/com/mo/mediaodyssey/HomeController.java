package com.mo.mediaodyssey;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mo.mediaodyssey.entity.Media;
import com.mo.mediaodyssey.repository.MediaRepository;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class HomeController {

    @Autowired
    private MediaRepository mediaRepository;

    @GetMapping("/")
    @ResponseBody
    public String home() {

        Media m = new Media();
        m.setTitle("Interstellar");
        m.setCategory("Movie");
        m.setViews(100);
        m.setLikes(50);
        m.setLikesLastWeek(30);

        mediaRepository.save(m);
        return "Saved!";
    }
}