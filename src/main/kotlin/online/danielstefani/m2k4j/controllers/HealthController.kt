package online.danielstefani.m2k4j.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/health")
class HealthController {
    @GetMapping
    fun healthCheck(): Mono<String> {
        return Mono.just("The cake is a lie. \uD83C\uDF82")
    }
}