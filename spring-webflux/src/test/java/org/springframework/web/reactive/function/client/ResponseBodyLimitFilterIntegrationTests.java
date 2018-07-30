package org.springframework.web.reactive.function.client;

import com.google.common.base.Strings;
import java.io.IOException;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ResponseBodyLimitFilterIntegrationTests {

	private static final int BODY_BYTES_LIMIT = 1500;

	private MockWebServer server;

	private WebClient webClient;

	@Before
	public void setup() {
		this.server = new MockWebServer();
		this.webClient = WebClient
				.builder()
				.baseUrl(this.server.url("/").toString())
				.filter(new ResponseBodyLimitFilterFunction(BODY_BYTES_LIMIT))
				.build();
	}

	@After
	public void shutdown() throws IOException {
		this.server.shutdown();
	}

	@Test
	public void responseSizeBelowLimit() {
		String shortResponse = Strings.repeat("1", 100);

		server.enqueue(new MockResponse().setBody(shortResponse));

		Mono<String> result = webClient
				.get()
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext(shortResponse)
				.expectComplete()
				.verify(Duration.ofSeconds(3));
	}

	@Test
	public void responseSizeAboveLimit() {
		server.enqueue(new MockResponse().setBody(Strings.repeat("1", BODY_BYTES_LIMIT * 1000)));

		Mono<String> result = webClient
				.get()
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext(Strings.repeat("1", BODY_BYTES_LIMIT))
				.expectComplete()
				.verify(Duration.ofSeconds(3));
	}
}
