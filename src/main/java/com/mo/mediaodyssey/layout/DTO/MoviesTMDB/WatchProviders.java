package com.mo.mediaodyssey.layout.DTO.MoviesTMDB;

public class WatchProviders {
    private String name; 
    private String logoUrl; 

    public WatchProviders() {}
    public WatchProviders(String name, String logoUrl) {
        this.name = name;
        this.logoUrl = logoUrl;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
    
}
