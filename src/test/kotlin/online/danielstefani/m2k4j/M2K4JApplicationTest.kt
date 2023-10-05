package online.danielstefani.m2k4j

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ActiveProfiles("test")
class M2K4JApplicationTest {

	@Autowired
	lateinit var restTemplate: TestRestTemplate

	@Test
	fun whenCallHealthCheck_thenShouldReturnOk() {
		val result = restTemplate.getForEntity("/health", String::class.java)
		assertEquals(HttpStatus.OK, result?.statusCode)
	}
}