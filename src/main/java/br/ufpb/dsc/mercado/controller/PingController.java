package br.ufpb.dsc.mercado.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class PingController {

    private static final String SERVICE_NAME = "eq13";

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok", SERVICE_NAME, Instant.now().toString());
    }

    public record PingResponse(String status, String service, String timestamp) {
    }
}
