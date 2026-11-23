/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Integration tests for {@link RestTestClient} against a live server.
 */
@ParameterizedClass
@MethodSource("clientHttpRequestFactories")
class RestTestClientIntegrationTests {

	static Stream<Arguments> clientHttpRequestFactories() {
		HttpComponentsClientHttpRequestFactory httpComponents = new HttpComponentsClientHttpRequestFactory();
		httpComponents.setConnectionRequestTimeout(Duration.ofSeconds(2));
		return Stream.of(
				argumentSet("JDK HttpURLConnection", new SimpleClientHttpRequestFactory()),
				argumentSet("Jetty", new JettyClientHttpRequestFactory()),
				argumentSet("JDK HttpClient", new JdkClientHttpRequestFactory()),
				argumentSet("Reactor Netty", new ReactorClientHttpRequestFactory()),
				argumentSet("HttpComponents", httpComponents)
		);
	}


	@AutoClose
	private final MockWebServer server = new MockWebServer();

	private final ClientHttpRequestFactory requestFactory;

	private final RestTestClient testClient;


	RestTestClientIntegrationTests(ClientHttpRequestFactory requestFactory) throws IOException {
		this.server.start();
		this.requestFactory = requestFactory;
		this.testClient = RestTestClient.bindToServer(requestFactory)
				.baseUrl(this.server.url("/").toString())
				.build();
	}

	@Test // gh-35784
	void sequentialRequestsNotConsumingBody() {
		for (int i = 0; i < 10; i++) {
			prepareResponse(builder ->
					builder.setHeader("Content-Type", "text/plain").body("Hello Spring!"));
			this.testClient.get().uri("/").exchange().expectStatus().isOk();
		}
	}

	@Test // gh-37732
	void sequentialRequestsGzipCompressedResponse() throws IOException {
		String expectedBody = "Hello Spring with gzip compression!".repeat(10);
		byte[] data = expectedBody.getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
			gzipOut.write(data);
		}
		byte[] gzipped = baos.toByteArray();

		for (int i = 0; i < 10; i++) {
			prepareResponse(builder -> {
				Buffer buffer = new Buffer().write(gzipped);
				return builder.setHeader("Content-Type", "text/plain")
						.setHeader("Content-Encoding", "gzip")
						.body(buffer);
			});
			if (this.requestFactory instanceof SimpleClientHttpRequestFactory) {
				this.testClient.get().uri("/").exchange()
						.expectStatus().isOk()
						.expectBody(byte[].class).isEqualTo(gzipped);
			}
			else {
				this.testClient.get().uri("/").exchange()
						.expectStatus().isOk()
						.expectBody(String.class).isEqualTo(expectedBody);
			}
		}
	}

	private void prepareResponse(Function<MockResponse.Builder, MockResponse.Builder> f) {
		MockResponse.Builder builder = new MockResponse.Builder();
		this.server.enqueue(f.apply(builder).build());
	}

}
