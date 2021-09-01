package com.cfgtest.services.gateway.api;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class CfgServiceGatewayController {

    @RequestMapping(value = "/fallback",
            method = RequestMethod.GET)
    public Mono<ResponseEntity<String>> fallback() {
        return Mono.just(ResponseEntity.status(200).body("Fallback"));
    }
}
