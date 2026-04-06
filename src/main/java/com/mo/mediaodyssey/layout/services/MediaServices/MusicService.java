package com.mo.mediaodyssey.layout.services.MediaServices;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mo.mediaodyssey.layout.DTO.MusicLASTFM.FMimage;
import com.mo.mediaodyssey.layout.DTO.MusicLASTFM.MusicResponse;
import com.mo.mediaodyssey.layout.models.BoardMedia;
import com.mo.mediaodyssey.layout.models.MediaModels.Music;

@Service
public class MusicService {

    @Value("${lastfm.api.key}")
    private String apiKey; 
    private final String baseURL = "http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=";

    private final RestTemplate restTemplate; 

    public MusicService (RestTemplate restTemplate) {
        this.restTemplate = restTemplate; 
    }

    // Translate HTTP to Json Object for DTO MusicResponse
    public MusicResponse getMusicByArtistTrack (String artist, String track){
        String url = baseURL + apiKey 
                + "&artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8)
                + "&track=" +  URLEncoder.encode(track, StandardCharsets.UTF_8)
                + "&format=json";

        return restTemplate.getForObject(url, MusicResponse.class); 
    }

    public List<Music> getMusicByBoardMediaList(List<BoardMedia> boardMediaList) {
        return boardMediaList.stream()
                .filter(m -> "music".equals(m.getMediaType()))
                .map(m -> convertToMusic(m.getArtist(), m.getTrack()))
                .collect(Collectors.toList());
    }

    //Since MusicResponse DTO can get messy, create a normal model to store the data easier & cleaner
    public Music convertToMusic (String artist, String track) {
        MusicResponse response = getMusicByArtistTrack(artist, track);

        // create Music object model from response: 
        Music music = new Music();

        //set the variables: 
        music.setTitle(response.getTrack().getName()); //name of the song (track)
        music.setArtist(response.getTrack().getArtist().getName()); //get the name of the artist
        music.setAlbum(response.getTrack().getAlbum().getTitle()); //get name of the album 

        // tags for the song
        if (response.getTrack().getTopTags() == null) {
            music.setGenres(List.of("Unknown"));
        }
        if (response.getTrack().getTopTags() != null &&
            response.getTrack().getTopTags().getTag() != null) {

            List<String> genres = response.getTrack().getTopTags()
                    .getTag().stream().map(tag -> tag.getName())
                    .collect(Collectors.toList());

            if (genres.isEmpty()) {
                genres = List.of("Unknown"); 
            }
            music.setGenres(genres);
        }

        //added double checking just in case details info of the song does not exist in last fm
        if (response.getTrack().getWiki() != null ) {
            music.setRelease_date(response.getTrack().getWiki().getRelease_date()); // get the release date of the song
            // Get summary of the song (also trim unnecessary details from JSON obj with cleanSummary())
            music.setOverview(cleanSummary(response.getTrack().getWiki().getSummary())); 
        }

        // set the poster URL (size extra large)
        List<FMimage> images = response.getTrack().getAlbum().getImage(); 
        music.setPoster_path(images.get(images.size()-1).getPoster_path()); //Get the png source for images (usually of the album)

        return music;
    }
    
    // Private functions: 
    // Trim unecessary elements in return JSON object 
    private String cleanSummary (String summary){
        if (summary == null) return "";

        int linkIndex = summary.indexOf("<a");
        if (linkIndex != -1) {
            summary = summary.substring(0, linkIndex);
        }
        return summary.replaceAll("<[^>]*>", "").trim();
    }
}
