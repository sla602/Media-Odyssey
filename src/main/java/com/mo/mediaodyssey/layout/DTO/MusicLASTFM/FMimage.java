package com.mo.mediaodyssey.layout.DTO.MusicLASTFM;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FMimage {
    @JsonProperty("#text")
    private String poster_path; 
    @JsonProperty("size")
    private String size; 

    public FMimage() {}; 
    public FMimage (String poster_path, String size) {
        this.poster_path = poster_path; 
        this.size = size; 
    }
    public String getPoster_path() {
        return poster_path;
    }
    public void setPoster_path(String poster_path) {
        this.poster_path = poster_path;
    }
    public String getSize() {
        return size;
    }
    public void setSize(String size) {
        this.size = size;
    }

    
}
