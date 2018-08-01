package org.springframework.web.reactive.function.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

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
	private static final String EXACT_BODY = Strings.repeat("1", BODY_BYTES_LIMIT);
	private static final String BIG_BODY = Strings.repeat("1", BODY_BYTES_LIMIT * 1000);

	private MockWebServer server;

	@Before
	public void setup() {
		this.server = new MockWebServer();
	}

	@After
	public void shutdown() throws IOException {
		this.server.shutdown();
	}

	@Test
	public void responseSizeBelowLimit_throwOnExceedConfigured() {
		enqueueResponse(EXACT_BODY);

		Mono<String> result = runWithFilter(new ResponseBodyLimitFilterFunction(BODY_BYTES_LIMIT, true));

		StepVerifier.create(result)
				.expectNext(EXACT_BODY)
				.expectComplete()
				.verify(Duration.ofSeconds(3));
	}

	@Test
	public void responseSizeBelowLimit_noThrowOnExceedConfigured() {
		enqueueResponse(EXACT_BODY);

		Mono<String> result = runWithFilter(new ResponseBodyLimitFilterFunction(BODY_BYTES_LIMIT, false));

		StepVerifier.create(result)
				.expectNext(EXACT_BODY)
				.expectComplete()
				.verify(Duration.ofSeconds(3));
	}

	@Test
	public void responseSizeAboveLimit_throwOnExceedConfigured() {
		enqueueResponse(BIG_BODY);

		Mono<String> result = runWithFilter(new ResponseBodyLimitFilterFunction(BODY_BYTES_LIMIT, true));

		StepVerifier.create(result)
				.expectErrorSatisfies(e -> assertThat(e, hasProperty("truncatedBody", equalTo(EXACT_BODY.getBytes()))))
				.verify(Duration.ofSeconds(3));
	}

	@Test
	public void responseSizeAboveLimit_noThrowOnExceedConfigured() {
		enqueueResponse(BIG_BODY);

		Mono<String> result = runWithFilter(new ResponseBodyLimitFilterFunction(BODY_BYTES_LIMIT, false));

		StepVerifier.create(result)
				.expectNext(EXACT_BODY)
				.expectComplete()
				.verify(Duration.ofSeconds(3));
	}

	private Mono<String> runWithFilter(ResponseBodyLimitFilterFunction filter) {
		WebClient webClient = WebClient
				.builder()
				.baseUrl(this.server.url("/").toString())
				.filter(filter)
				.build();

		return webClient
				.get()
				.retrieve()
				.bodyToMono(String.class);
	}

	private void enqueueResponse(String body) {
		server.enqueue(new MockResponse().setBody(body));
	}
}
