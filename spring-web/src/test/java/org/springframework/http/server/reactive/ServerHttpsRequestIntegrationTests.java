/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpsServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;

/**
 * HTTPS-specific integration test for {@link ServerHttpRequest}.
 * @author Arjen Poutsma
 */
@RunWith(Parameterized.class)
public class ServerHttpsRequestIntegrationTests {

	private int port;

	@Parameterized.Parameter(0)
	public HttpServer server;

	private RestTemplate restTemplate;

	@Parameterized.Parameters(name = "server [{0}]")
	public static Object[][] arguments() {
		return new Object[][]{
				{new ReactorHttpsServer()},
		};
	}

	@Before
	public void setup() throws Exception {
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

	@After
	public void tearDown() throws Exception {
		this.server.stop();
		this.port = 0;
	}

	@Test
	public void checkUri() throws Exception {
		URI url = new URI("https://localhost:" + port + "/foo?param=bar");
		RequestEntity<Void> request = RequestEntity.post(url).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	public static class CheckRequestHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			URI uri = request.getURI();
			assertEquals("https", uri.getScheme());
			assertNotNull(uri.getHost());
			assertNotEquals(-1, uri.getPort());
			assertNotNull(request.getRemoteAddress());
			assertEquals("/foo", uri.getPath());
			assertEquals("param=bar", uri.getQuery());
			return Mono.empty();
		}
	}

}
