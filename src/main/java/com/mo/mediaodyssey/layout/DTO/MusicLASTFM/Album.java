package com.mo.mediaodyssey.layout.DTO.MusicLASTFM;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Album {
    private String title; 
    
    @JsonProperty("image")
    private List<FMimage> image; 
    
    public Album() {}; 
    public Album (String title, List<FMimage> image) {
        this.title = title; 
        this.image = image; 
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public List<FMimage> getImage() {
        return image;
    }
    public void setImage(List<FMimage> image) {
        this.image = image;
    }
    
}
