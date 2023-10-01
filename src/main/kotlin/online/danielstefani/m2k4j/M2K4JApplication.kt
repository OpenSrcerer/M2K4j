package online.danielstefani.m2k4j

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class M2K4JApplication

fun main(args: Array<String>) {
	runApplication<M2K4JApplication>(*args)
}
