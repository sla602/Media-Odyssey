package com.mo.mediaodyssey.layout.DTO.MoviesTMDB;

public class WatchProviders {
    private String provider_name; 
    private String logo_url; 

    public WatchProviders() {}
    public WatchProviders(String provider_name, String logo_url) {
        this.provider_name = provider_name;
        this.logo_url = logo_url;
    }
    public String getProvider_name() {
        return provider_name;
    }
    public void setProvider_name(String provider_name) {
        this.provider_name = provider_name;
    }
    public String getLogo_url() {
        return logo_url;
    }
    public void setLogo_url(String logo_url) {
        this.logo_url = logo_url;
    }
    
}
