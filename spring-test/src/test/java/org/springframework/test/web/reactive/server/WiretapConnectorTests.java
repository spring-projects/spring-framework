/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
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
public class WiretapConnectorTests {

	@Test
	public void captureAndClaim() {
		ClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/test");
		ClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		ClientHttpConnector connector = (method, uri, fn) -> fn.apply(request).then(Mono.just(response));

		ClientRequest clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("/test"))
				.header(WebTestClient.WEBTESTCLIENT_REQUEST_ID, "1").build();

		WiretapConnector wiretapConnector = new WiretapConnector(connector);
		ExchangeFunction function = ExchangeFunctions.create(wiretapConnector);
		function.exchange(clientRequest).block(ofMillis(0));

		ExchangeResult result = wiretapConnector.getExchangeResult("1", null, Duration.ZERO);
		assertThat(result.getMethod()).isEqualTo(HttpMethod.GET);
		assertThat(result.getUrl().toString()).isEqualTo("/test");
	}

}
