package com.movieapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.JedisPooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class ApiRestController {

    private static final Logger logger = LoggerFactory.getLogger(ApiRestController.class);

    // Retrieve redis host and port from application.properties
    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private Integer redisPort;

    private JedisPooled r;

    @PostConstruct
    public void init() {
        r = new JedisPooled(redisHost, redisPort);
    }

    @GetMapping(value = "/")
    public String doGet(@RequestParam(required = false) String user_id) {
        if (user_id == null) {
            return "Hello World!";
        } else {
            logger.info("Getting favorites for user " + user_id);

            List<String> favorites = new ArrayList<>(r.smembers(user_id));
            JSONObject favorites_json = new JSONObject();
            favorites_json.put("favorites", favorites);

            logger.info("User " + user_id + " has favorites " + favorites);

            return favorites_json.toString();
        }
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String doPost(@RequestParam String user_id, @RequestBody JSONObject json) {

        logger.info("Adding or removing favorites");
        String movieID = Integer.toString(json.getInt("id"));

        logger.info("Adding or removing favorites for user " +  user_id + ", movieID " + movieID);

        Long redisResponse = r.srem(user_id, movieID);
        if (redisResponse == 0) {
            r.sadd(user_id, movieID);
        }

        logger.info("Getting favorites for user " + user_id);

        List<String> favorites = new ArrayList<>(r.smembers(user_id));
        JSONObject favorites_json = new JSONObject();
        favorites_json.put("favorites", favorites);

        logger.info("User " + user_id + " has favorites " + favorites);

        return favorites_json.toString();
    }
}
