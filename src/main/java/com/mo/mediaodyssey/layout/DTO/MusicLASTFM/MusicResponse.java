package com.mo.mediaodyssey.layout.DTO.MusicLASTFM;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MusicResponse {
    private Track track; 

    public Track getTrack() {return track;}
    public void setTrack(Track track) { this.track = track;}

    public static class Track {
        private String name; 

        private Album album; 
        private Artist artist; 
        private Wiki wiki; 

        @JsonProperty("toptags")
        private TopTags topTags;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Album getAlbum() {
            return album;
        }

        public void setAlbum(Album album) {
            this.album = album;
        }

        public Artist getArtist() {
            return artist;
        }

        public void setArtist(Artist artist) {
            this.artist = artist;
        }

        public Wiki getWiki() {
            return wiki;
        }

        public void setWiki(Wiki wiki) {
            this.wiki = wiki;
        }

        public TopTags getTopTags() {
            return topTags;
        }

        public void setTopTags(TopTags topTags) {
            this.topTags = topTags;
        }
    }

    // Smaller response for MusicLastFM
    public static class Artist {
        private String name; 

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }; 
    }

    public static class Wiki {
        
        @JsonProperty("published")
        private String release_date; 
        private String summary;

        public String getRelease_date() {
            return release_date;
        }
        public void setRelease_date(String release_date) {
            this.release_date = release_date;
        }
        public String getSummary() {
            return summary;
        }
        public void setSummary(String summary) {
            this.summary = summary;
        } 
    }
    
    public static class TopTags {
        private List<FMtags> tags;

        public List<FMtags> getTags() {
            return tags;
        }
        public void setTags(List<FMtags> tags) {
            this.tags = tags;
        } 
    }

    public static class FMtags {
        private String name;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }
}
