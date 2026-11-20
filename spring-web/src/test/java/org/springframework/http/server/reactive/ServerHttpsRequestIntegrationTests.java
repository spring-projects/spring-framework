/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.net.URI;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.ReactorHttpsServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTPS-specific integration test for {@link ServerHttpRequest}.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
class ServerHttpsRequestIntegrationTests {

	private final HttpServer server = new ReactorHttpsServer();

	private RestClient restClient;

	@BeforeEach
	void startServer() throws Exception {
		this.server.setHandler(new CheckRequestHandler());
		this.server.afterPropertiesSet();
		this.server.start();


		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(new TrustSelfSignedStrategy());
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				builder.build(), NoopHostnameVerifier.INSTANCE);
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setSSLSocketFactory(socketFactory)
				.build();
		CloseableHttpClient httpclient = HttpClients.custom().
				setConnectionManager(connectionManager).build();
		HttpComponentsClientHttpRequestFactory requestFactory =
				new HttpComponentsClientHttpRequestFactory(httpclient);
		this.restClient = RestClient.builder().baseUrl("https://localhost:" + this.server.getPort())
				.requestFactory(requestFactory).build();
	}

	@AfterEach
	void stopServer() {
		this.server.stop();
	}

	@Test
	void checkUri() {
		ResponseEntity<Void> response = this.restClient.post().uri("/foo?param=bar").retrieve().toBodilessEntity();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}


	private static class CheckRequestHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			URI uri = request.getURI();
			assertThat(uri.getScheme()).isEqualTo("https");
			assertThat(uri.getHost()).isNotNull();
			assertThat(uri.getPort()).isNotEqualTo(-1);
			assertThat(request.getRemoteAddress()).isNotNull();
			assertThat(uri.getPath()).isEqualTo("/foo");
			assertThat(uri.getQuery()).isEqualTo("param=bar");
			return Mono.empty();
		}
	}

}
