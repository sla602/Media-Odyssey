package com.mo.mediaodyssey.layout.services.MediaServices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    public String getGameById(Long gameId) {
        String url = base_URL + "games/" + gameId + "?key=" + apiKey; 
        RestTemplate restTemplate = new RestTemplate(); 

        return restTemplate.getForObject(url, String.class); 
    }
}
