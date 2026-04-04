package com.mo.mediaodyssey.layout.DTO.GamesRAWG;

import java.util.List;

public class GamesDTO {
    private String name; 
    private String description; 
    private String background_image; 
    private List<String> genres; 
    private String publishers;
    
    public GamesDTO() {}; 
    public GamesDTO(String name, String description, String background_image, List<String> genres, String publishers) {
        this.name = name; 
        this.description = description; 
        this.background_image = background_image; 
        this.genres = genres; 
        this.publishers = publishers;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getBackground_image() {
        return background_image;
    }
    public void setBackground_image(String background_image) {
        this.background_image = background_image;
    }
    public List<String> getGenres() {
        return genres;
    }
    public void setGenres(List<String> genres) {
        this.genres = genres;
    }
    public String getPublishers() {
        return publishers;
    }
    public void setPublishers(String publishers) {
        this.publishers = publishers;
    }
}
