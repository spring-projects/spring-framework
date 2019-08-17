/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.net.URI;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpsServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTPS-specific integration test for {@link ServerHttpRequest}.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class ServerHttpsRequestIntegrationTests {

	private final HttpServer server = new ReactorHttpsServer();

	private int port;

	private RestTemplate restTemplate;


	@BeforeEach
	void startServer() throws Exception {
		this.server.setHandler(new CheckRequestHandler());
		this.server.afterPropertiesSet();
		this.server.start();

		// Set dynamically chosen port
		this.port = this.server.getPort();

		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(new TrustSelfSignedStrategy());
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				builder.build(), NoopHostnameVerifier.INSTANCE);
		CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(
				socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory =
				new HttpComponentsClientHttpRequestFactory(httpclient);
		this.restTemplate = new RestTemplate(requestFactory);
	}

	@AfterEach
	void stopServer() {
		this.server.stop();
	}

	@Test
	void checkUri() throws Exception {
		URI url = new URI("https://localhost:" + port + "/foo?param=bar");
		RequestEntity<Void> request = RequestEntity.post(url).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}


	private static class CheckRequestHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			URI uri = request.getURI();
			assertThat(uri.getScheme()).isEqualTo("https");
			assertThat(uri.getHost()).isNotNull();
			assertThat(uri.getPort()).isNotEqualTo((long) -1);
			assertThat(request.getRemoteAddress()).isNotNull();
			assertThat(uri.getPath()).isEqualTo("/foo");
			assertThat(uri.getQuery()).isEqualTo("param=bar");
			return Mono.empty();
		}
	}

}
