package online.danielstefani.m2k4k.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/")
class HealthController {
    @GetMapping
    fun healthCheck(): Mono<String> {
        return Mono.just("Oooooh-ooh! I'm still alive, yeeeeeeaaaahaaa!")
    }
}