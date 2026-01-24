package dev.auctoritas.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GatewayServiceApplicationTests {

	@LocalServerPort
	private int port;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		webTestClient = WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + port)
			.build();
	}

	@Test
	void contextLoads() {
		webTestClient.get()
			.uri("/actuator/health")
			.exchange()
			.expectStatus()
			.is2xxSuccessful();
	}

}
