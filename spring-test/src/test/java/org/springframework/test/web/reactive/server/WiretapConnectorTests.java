/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.testfixture.io.buffer.LeakAwareDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeFunctions;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WiretapConnector}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class WiretapConnectorTests {

	private final LeakAwareDataBufferFactory bufferFactory =
			new LeakAwareDataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT));

	@AfterEach
	void tearDown() {
		this.bufferFactory.checkForLeaks();
	}

	@Test
	void captureAndClaim() {
		ClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/test");
		ClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		ClientHttpConnector connector = createConnector(request, response);

		ClientRequest clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("/test"))
				.header(WebTestClient.WEBTESTCLIENT_REQUEST_ID, "1").build();

		WiretapConnector wiretapConnector = new WiretapConnector(connector, null);
		ExchangeFunction function = ExchangeFunctions.create(wiretapConnector);
		function.exchange(clientRequest).block(ofMillis(0));

		ExchangeResult result = wiretapConnector.getExchangeResult("1", null, Duration.ZERO);
		assertThat(result.getMethod()).isEqualTo(HttpMethod.GET);
		assertThat(result.getUrl().toString()).isEqualTo("/test");
	}

	@Test
	void shouldReleaseBuffers() {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/test");
		MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		response.setBody(Flux.just(toDataBuffer("Hello Spring")));
		ClientHttpConnector connector = createConnector(request, response);

		ClientRequest clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("/test"))
				.header(WebTestClient.WEBTESTCLIENT_REQUEST_ID, "1").build();

		WiretapConnector wiretapConnector = new WiretapConnector(connector, null);
		ExchangeFunction function = ExchangeFunctions.create(wiretapConnector);
		function.exchange(clientRequest).block(ofMillis(0));
		ExchangeResult result = wiretapConnector.getExchangeResult("1", null, Duration.ZERO);
		result.getResponseBodyContent();
	}

	@Test
	void shouldReleaseBuffersOnlyOnce() {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/test");
		MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		response.setBody(Flux.just(toDataBuffer("Hello Spring"), toDataBuffer("Hello Spring"), toDataBuffer("Hello Spring"), toDataBuffer("Hello Spring")));
		ClientHttpConnector connector = createConnector(request, response);

		ClientRequest clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("/test"))
				.header(WebTestClient.WEBTESTCLIENT_REQUEST_ID, "1").build();

		WiretapConnector wiretapConnector = new WiretapConnector(connector, null);
		ExchangeFunction function = ExchangeFunctions.create(wiretapConnector);
		function.exchange(clientRequest).flatMap(ClientResponse::releaseBody).block(ofMillis(0));
		ExchangeResult result = wiretapConnector.getExchangeResult("1", null, Duration.ZERO);
		result.getResponseBodyContent();
	}

	private ClientHttpConnector createConnector(ClientHttpRequest request, ClientHttpResponse response) {
		return (method, uri, fn) -> fn.apply(request).then(Mono.just(response));
	}

	private DataBuffer toDataBuffer(String s) {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(256);
		buffer.write(s.getBytes(StandardCharsets.UTF_8));
		return buffer;
	}
}
