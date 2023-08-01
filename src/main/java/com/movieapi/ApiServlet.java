package com.movieapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.JedisPool;
import javax.annotation.PostConstruct;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;


@RestController
@RequestMapping("/")
public class ApiServlet {
    private static final Logger logger = LogManager.getLogger(ApiServlet.class);

    // Create artificial delay if set
    @Value("${TOGGLE_SERVICE_DELAY:0}")
    private Integer delayTime;

    // Create redis pool using Jedis client
    @Value("${REDIS_HOST:localhost}")
    private String redisHost;

    @Value("${REDIS_PORT:6379}")
    private Integer redisPort;


    private JedisPool r;

    @PostConstruct
    public void init() {
        r = new JedisPool(redisHost, redisPort);
    }

    @GetMapping
    public String helloWorld(@RequestParam(required = false) String user_id) throws InterruptedException {
        Span span = GlobalOpenTelemetry.getTracer("YEAH").spanBuilder("helloWorld").startSpan();
        Scope scope = span.makeCurrent();

        if (user_id == null) {

            logger.info("Main request successful");
            span.addEvent("a span event", Attributes
                    .of(AttributeKey.longKey("someKey"), Long.valueOf(93)));

            span.setStatus(StatusCode.OK);

            span.end();

            scope.close();
            return "Hello World!";
        } else {
            span.addEvent("a span event", Attributes
                    .of(AttributeKey.longKey("someKey"), Long.valueOf(93)));

            span.setStatus(StatusCode.OK);

            span.end();

            scope.close();
            return getUserFavorites(user_id);
        }


    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String handlePost(@RequestParam String user_id, @RequestBody JSONObject requestBody) throws InterruptedException, Exception {
        handleDelay();
        logger.info("Adding or removing favorites");
        String movieID = Integer.toString(requestBody.getInt("id"));

        logger.info("Adding or removing favorites for user " +  user_id + ", movieID " + movieID);

        Long redisResponse = r.getResource().srem(user_id, movieID);
        if (redisResponse == 0) {
            r.getResource().sadd(user_id, movieID);
        }

        String favorites = getUserFavorites(user_id);

        handleCanary();

        return favorites;
    }

    private String getUserFavorites(String user_id) throws InterruptedException {
        handleDelay();

        logger.info("Getting favorites for user " + user_id);

        List<String> favorites = new ArrayList<>(r.getResource().smembers(user_id));
        JSONObject favorites_json = new JSONObject();
        favorites_json.put("favorites", favorites);

        logger.info("User " + user_id + " has favorites " + favorites);

        return favorites_json.toString();
    }

    private void handleDelay() throws InterruptedException {
        if (delayTime > 0) {
            Random random = new Random();
            double randomGaussDelay =
                    Math.max(0, random.nextGaussian() * (delayTime / 1000 / 10) + (delayTime / 1000));
            TimeUnit.MILLISECONDS.sleep((long) randomGaussDelay);
        }
    }

    private void handleCanary() throws Exception {
        Span span = GlobalOpenTelemetry.getTracer("YEAH").spanBuilder("handleCanary").startSpan();
        Scope scope = span.makeCurrent();

        Integer sleepTime =
                Integer.parseInt(Objects.requireNonNullElse(System.getenv("TOGGLE_CANARY_DELAY"), "0"));
        Random random = new Random();
        if (sleepTime > 0 && random.nextDouble() < 0.5) {
            double randomGaussDelay =
                    Math.max(0, random.nextGaussian() * (sleepTime / 1000 / 10) + (sleepTime / 1000));
            TimeUnit.MILLISECONDS.sleep((long) randomGaussDelay);
            logger.info("Canary enabled");
            Span.current().setAttribute("canary", "test-new-feature");
            Span.current().setAttribute("quiz_solution", "correlations");
            Double toggleCanaryFailure =
                    Double.parseDouble(Objects.requireNonNullElse(System.getenv("TOGGLE_CANARY_FAILURE"), "0"));
            if (random.nextDouble() < toggleCanaryFailure) {
                logger.error("Something went wrong");
                throw new Exception("Something went wrong");
            }
        }
            span.addEvent("a span event", Attributes
                    .of(AttributeKey.longKey("someKey"), Long.valueOf(93)));

                                span.setStatus(StatusCode.OK);

        span.end();

        scope.close();
    }
}
