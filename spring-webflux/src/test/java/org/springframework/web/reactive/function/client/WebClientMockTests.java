package org.springframework.web.reactive.function.client;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.http.client.reactive.test.MockClientHttpResponse;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Mock tests using a {@link ExchangeFunction} through {@link WebClient}.
 *
 * @author Brian Clozel
 */
public class WebClientMockTests {

	private MockClientHttpResponse response;

	private ClientHttpConnector mockConnector;

	private WebClient webClient;

	@Before
	public void setUp() throws Exception {
		this.mockConnector = mock(ClientHttpConnector.class);
		this.webClient = WebClient.builder().clientConnector(this.mockConnector).build();
		this.response = new MockClientHttpResponse(HttpStatus.OK);
		this.response.setBody("example");

		given(this.mockConnector.connect(any(), any(), any())).willReturn(Mono.just(this.response));
	}

	@Test
	public void shouldDisposeResponseManually() {
		Mono<HttpHeaders> headers = this.webClient
				.get().uri("/test")
				.exchange()
				.map(response -> response.headers().asHttpHeaders());
		StepVerifier.create(headers)
				.expectNextCount(1)
				.verifyComplete();
		assertFalse(this.response.isClosed());
	}

}
