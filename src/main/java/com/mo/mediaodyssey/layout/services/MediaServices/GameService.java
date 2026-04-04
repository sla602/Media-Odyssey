package com.mo.mediaodyssey.layout.services.MediaServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mo.mediaodyssey.layout.DTO.GamesRAWG.GameResponse;

@Service
public class GameService {
    private final RestTemplate restTemplate; 

    @Value("${rawg.api.key}")
    private String apiKey; 
    private final String base_URL = "https://api.rawg.io/api/"; 

    public GameService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate; 
    } 

    // This function is used to translate the web result to json object.
    public GameResponse getGameById(Long gameId) {
        String url = base_URL + "games/" + gameId + "?key=" + apiKey; 

        return restTemplate.getForObject(url, GameResponse.class); 
    }

    // This function will assist in getting a list of games instead of just one. (for display in theme boards)
    public List<GameResponse> getGamesByIds (List<Long> mediaApiIds) {

        List<GameResponse> games = new ArrayList<>(); 

        for (Long id: mediaApiIds) {
            GameResponse game = getGameById(id); 
            if(game != null) {
                games.add(game);
            }
        }

        return games;
    }
}
