/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using an {@link ExchangeFunction} through {@link WebClient}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author Denys Ivano
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Martin Tarjányi
 */
class WebClientIntegrationTests {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("arguments")
	@interface ParameterizedWebClientTest {
	}

	static Stream<Named<ClientHttpConnector>> arguments() {
		return Stream.of(
				Named.named("Reactor Netty", new ReactorClientHttpConnector()),
				Named.named("Jetty", new JettyClientHttpConnector()),
				Named.named("HttpComponents", new HttpComponentsClientHttpConnector())
		);
	}


	private MockWebServer server;

	private WebClient webClient;


	private void startServer(ClientHttpConnector connector) {
		this.server = new MockWebServer();
		this.webClient = WebClient
				.builder()
				.clientConnector(connector)
				.baseUrl(this.server.url("/").toString())
				.build();
	}

	@AfterEach
	void shutdown() throws IOException {
		if (server != null) {
			this.server.shutdown();
		}
	}


	@ParameterizedWebClientTest
	void retrieve(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response ->
				response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting")
				.cookie("testkey", "testvalue")
				.header("X-Test-Header", "testvalue")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.COOKIE)).isEqualTo("testkey=testvalue");
			assertThat(request.getHeader("X-Test-Header")).isEqualTo("testvalue");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void retrieveJson(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

		Mono<Pojo> result = this.webClient.get()
				.uri("/pojo")
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(Pojo.class);

		StepVerifier.create(result)
				.expectNext(new Pojo("foofoo", "barbar"))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/pojo");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieveJsonWithParameterizedTypeReference(ClientHttpConnector connector) {
		startServer(connector);

		String content = "{\"containerValue\":{\"bar\":\"barbar\",\"foo\":\"foofoo\"}}";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<ValueContainer<Pojo>> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<ValueContainer<Pojo>>() {});

		StepVerifier.create(result)
				.assertNext(c -> assertThat(c.getContainerValue()).isEqualTo(new Pojo("foofoo", "barbar")))
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieveJsonAsResponseEntity(ClientHttpConnector connector) {
		startServer(connector);

		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<ResponseEntity<String>> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntity(String.class);

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
					assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
					assertThat(entity.getHeaders().getContentLength()).isEqualTo(31);
					assertThat(entity.getBody()).isEqualTo(content);
				})
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieveJsonAsBodilessEntity(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

		Mono<ResponseEntity<Void>> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toBodilessEntity();

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
					assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
					assertThat(entity.getHeaders().getContentLength()).isEqualTo(31);
					assertThat(entity.getBody()).isNull();
				})
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieveJsonArray(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json")
				.setBody("[{\"bar\":\"bar1\",\"foo\":\"foo1\"},{\"bar\":\"bar2\",\"foo\":\"foo2\"}]"));

		Flux<Pojo> result = this.webClient.get()
				.uri("/pojos")
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToFlux(Pojo.class);

		StepVerifier.create(result)
				.consumeNextWith(p -> assertThat(p.getBar()).isEqualTo("bar1"))
				.consumeNextWith(p -> assertThat(p.getBar()).isEqualTo("bar2"))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/pojos");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieveJsonArrayAsResponseEntityList(ClientHttpConnector connector) {
		startServer(connector);

		String content = "[{\"bar\":\"bar1\",\"foo\":\"foo1\"}, {\"bar\":\"bar2\",\"foo\":\"foo2\"}]";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<ResponseEntity<List<Pojo>>> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntityList(Pojo.class);

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
					assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
					assertThat(entity.getHeaders().getContentLength()).isEqualTo(58);
					Pojo pojo1 = new Pojo("foo1", "bar1");
					Pojo pojo2 = new Pojo("foo2", "bar2");
					assertThat(entity.getBody()).isEqualTo(Arrays.asList(pojo1, pojo2));
				})
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieveJsonArrayAsResponseEntityFlux(ClientHttpConnector connector) {
		startServer(connector);

		String content = "[{\"bar\":\"bar1\",\"foo\":\"foo1\"}, {\"bar\":\"bar2\",\"foo\":\"foo2\"}]";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		ResponseEntity<Flux<Pojo>> entity = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntityFlux(Pojo.class)
				.block(Duration.ofSeconds(3));

		assertThat(entity).isNotNull();
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(58);

		assertThat(entity.getBody()).isNotNull();
		StepVerifier.create(entity.getBody())
				.expectNext(new Pojo("foo1", "bar1"))
				.expectNext(new Pojo("foo2", "bar2"))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieveJsonArrayAsResponseEntityFluxWithBodyExtractor(ClientHttpConnector connector) {
		startServer(connector);

		String content = "[{\"bar\":\"bar1\",\"foo\":\"foo1\"}, {\"bar\":\"bar2\",\"foo\":\"foo2\"}]";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		ResponseEntity<Flux<Pojo>> entity = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntityFlux(BodyExtractors.toFlux(Pojo.class))
				.block(Duration.ofSeconds(3));

		assertThat(entity).isNotNull();
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(58);

		assertThat(entity.getBody()).isNotNull();
		StepVerifier.create(entity.getBody())
				.expectNext(new Pojo("foo1", "bar1"))
				.expectNext(new Pojo("foo2", "bar2"))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@Test // gh-24788
	void retrieveJsonArrayAsBodilessEntityShouldReleasesConnection() {

		// Constrain connection pool and make consecutive requests.
		// 2nd request should hang if response was not drained.

		ConnectionProvider connectionProvider = ConnectionProvider.create("test", 1);

		this.server = new MockWebServer();
		WebClient webClient = WebClient
				.builder()
				.clientConnector(new ReactorClientHttpConnector(HttpClient.create(connectionProvider)))
				.baseUrl(this.server.url("/").toString())
				.build();

		for (int i=1 ; i <= 2; i++) {

			// Response must be large enough to circumvent eager prefetching

			String json = Flux.just("{\"bar\":\"bar\",\"foo\":\"foo\"}")
					.repeat(100)
					.collect(Collectors.joining(",", "[", "]"))
					.block();

			prepareResponse(response -> response
					.setHeader("Content-Type", "application/json")
					.setBody(json));

			Mono<ResponseEntity<Void>> result = webClient.get()
					.uri("/json").accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.toBodilessEntity();

			StepVerifier.create(result)
					.consumeNextWith(entity -> {
						assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
						assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
						assertThat(entity.getHeaders().getContentLength()).isEqualTo(2627);
						assertThat(entity.getBody()).isNull();
					})
					.expectComplete()
					.verify(Duration.ofSeconds(3));

			expectRequestCount(i);
			expectRequest(request -> {
				assertThat(request.getPath()).isEqualTo("/json");
				assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
			});
		}
	}

	@ParameterizedWebClientTest
	void retrieveJsonAsSerializedText(ClientHttpConnector connector) {
		startServer(connector);

		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<String> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext(content)
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	@SuppressWarnings("rawtypes")
	void retrieveJsonNull(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response
				.setResponseCode(200)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody("null"));

		Mono<Map> result = this.webClient.get()
				.uri("/null")
				.retrieve()
				.bodyToMono(Map.class);

		StepVerifier.create(result).expectComplete().verify(Duration.ofSeconds(3));
	}

	@ParameterizedWebClientTest  // SPR-15946
	void retrieve404(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(404)
				.setHeader("Content-Type", "text/plain"));

		Mono<String> result = this.webClient.get().uri("/greeting")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(WebClientResponseException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void retrieve404WithBody(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(404)
				.setHeader("Content-Type", "text/plain").setBody("Not Found"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(WebClientResponseException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void retrieve500(ClientHttpConnector connector) {
		startServer(connector);

		String errorMessage = "Internal Server error";
		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody(errorMessage));

		String path = "/greeting";
		Mono<String> result = this.webClient.get()
				.uri(path)
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectErrorSatisfies(throwable -> {
					assertThat(throwable instanceof WebClientResponseException).isTrue();
					WebClientResponseException ex = (WebClientResponseException) throwable;
					assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
					assertThat(ex.getRawStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
					assertThat(ex.getStatusText()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
					assertThat(ex.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
					assertThat(ex.getResponseBodyAsString()).isEqualTo(errorMessage);

					HttpRequest request = ex.getRequest();
					assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
					assertThat(request.getURI()).isEqualTo(URI.create(this.server.url(path).toString()));
					assertThat(request.getHeaders()).isNotNull();
				})
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo(path);
		});
	}

	@ParameterizedWebClientTest
	void retrieve500AsEntity(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Mono<ResponseEntity<String>> result = this.webClient.get()
				.uri("/").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntity(String.class);

		StepVerifier.create(result)
				.expectError(WebClientResponseException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieve500AsEntityList(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Mono<ResponseEntity<List<String>>> result = this.webClient.get()
				.uri("/").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntityList(String.class);

		StepVerifier.create(result)
				.expectError(WebClientResponseException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieve500AsBodilessEntity(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Mono<ResponseEntity<Void>> result = this.webClient.get()
				.uri("/").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toBodilessEntity();

		StepVerifier.create(result)
				.expectError(WebClientResponseException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void retrieve555UnknownStatus(ClientHttpConnector connector) {
		startServer(connector);

		int errorStatus = 555;
		assertThat((Object) HttpStatus.resolve(errorStatus)).isNull();
		String errorMessage = "Something went wrong";
		prepareResponse(response -> response.setResponseCode(errorStatus)
				.setHeader("Content-Type", "text/plain").setBody(errorMessage));

		Mono<String> result = this.webClient.get()
				.uri("/unknownPage")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectErrorSatisfies(throwable -> {
					assertThat(throwable instanceof UnknownHttpStatusCodeException).isTrue();
					UnknownHttpStatusCodeException ex = (UnknownHttpStatusCodeException) throwable;
					assertThat(ex.getMessage()).isEqualTo(("Unknown status code ["+errorStatus+"]"));
					assertThat(ex.getRawStatusCode()).isEqualTo(errorStatus);
					assertThat(ex.getStatusText()).isEqualTo("");
					assertThat(ex.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
					assertThat(ex.getResponseBodyAsString()).isEqualTo(errorMessage);
				})
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/unknownPage");
		});
	}

	@ParameterizedWebClientTest
	void postPojoAsJson(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setHeader("Content-Type", "application/json")
				.setBody("{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}"));

		Mono<Pojo> result = this.webClient.post()
				.uri("/pojo/capitalize")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(new Pojo("foofoo", "barbar"))
				.retrieve()
				.bodyToMono(Pojo.class);

		StepVerifier.create(result)
				.consumeNextWith(p -> assertThat(p.getBar()).isEqualTo("BARBAR"))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/pojo/capitalize");
			assertThat(request.getBody().readUtf8()).isEqualTo("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}");
			assertThat(request.getHeader(HttpHeaders.CONTENT_LENGTH)).isEqualTo("31");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
			assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest  // SPR-16246
	void postLargeTextFile(ClientHttpConnector connector) throws Exception {
		startServer(connector);

		prepareResponse(response -> {});

		Resource resource = new ClassPathResource("largeTextFile.txt", getClass());
		Flux<DataBuffer> body = DataBufferUtils.read(resource, DefaultDataBufferFactory.sharedInstance, 4096);

		Mono<Void> result = this.webClient.post()
				.uri("/")
				.body(body, DataBuffer.class)
				.retrieve()
				.bodyToMono(Void.class);

		StepVerifier.create(result)
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		expectRequest(request -> {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				request.getBody().copyTo(bos);
				String actual = bos.toString("UTF-8");
				String expected = new String(Files.readAllBytes(resource.getFile().toPath()), StandardCharsets.UTF_8);
				assertThat(actual).isEqualTo(expected);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});
	}

	@ParameterizedWebClientTest
	void statusHandler(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting")
				.retrieve()
				.onStatus(HttpStatus::is5xxServerError, response -> Mono.just(new MyException("500 error!")))
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(MyException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void statusHandlerParameterizedTypeReference(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting")
				.retrieve()
				.onStatus(HttpStatus::is5xxServerError, response -> Mono.just(new MyException("500 error!")))
				.bodyToMono(new ParameterizedTypeReference<String>() {});

		StepVerifier.create(result)
				.expectError(MyException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void statusHandlerWithErrorBodyTransformation(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response
				.setResponseCode(500)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}")
		);

		Mono<String> result = this.webClient.get()
				.uri("/json")
				.retrieve()
				.onStatus(HttpStatus::isError,
						response -> response.bodyToMono(Pojo.class)
								.flatMap(pojo -> Mono.error(new MyException(pojo.getFoo())))
				)
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeErrorWith(throwable -> {
					assertThat(throwable).isInstanceOf(MyException.class);
					MyException error = (MyException) throwable;
					assertThat(error.getMessage()).isEqualTo("foofoo");
				})
				.verify(Duration.ofSeconds(3));
	}

	@ParameterizedWebClientTest
	void statusHandlerRawStatus(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting")
				.retrieve()
				.onRawStatus(value -> value >= 500 && value < 600, response -> Mono.just(new MyException("500 error!")))
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(MyException.class)
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void statusHandlerSuppressedErrorSignal(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting")
				.retrieve()
				.onStatus(HttpStatus::is5xxServerError, response -> Mono.empty())
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("Internal Server error")
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void statusHandlerSuppressedErrorSignalWithFlux(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

		Flux<String> result = this.webClient.get()
				.uri("/greeting")
				.retrieve()
				.onStatus(HttpStatus::is5xxServerError, response -> Mono.empty())
				.bodyToFlux(String.class);

		StepVerifier.create(result)
				.expectNext("Internal Server error")
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void statusHandlerSuppressedErrorSignalWithEntity(ClientHttpConnector connector) {
		startServer(connector);

		String content = "Internal Server error";
		prepareResponse(response -> response.setResponseCode(500)
				.setHeader("Content-Type", "text/plain").setBody(content));

		Mono<ResponseEntity<String>> result = this.webClient.get()
				.uri("/").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.onStatus(HttpStatus::is5xxServerError, response -> Mono.empty())// use normal response
				.toEntity(String.class);

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
					assertThat(entity.getBody()).isEqualTo(content);
				})
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void exchangeForPlainText(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setBody("Hello Spring!"));

		Mono<String> result = this.webClient.get()
				.uri("/greeting")
				.header("X-Test-Header", "testvalue")
				.retrieve().bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader("X-Test-Header")).isEqualTo("testvalue");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void exchangeForJsonAsResponseEntity(ClientHttpConnector connector) {
		startServer(connector);

		String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<ResponseEntity<Pojo>> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve().toEntity(Pojo.class);

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
					assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
					assertThat(entity.getHeaders().getContentLength()).isEqualTo(31);
					assertThat(entity.getBody()).isEqualTo(new Pojo("foofoo", "barbar"));
				})
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void exchangeForJsonAsBodilessEntity(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

		Mono<ResponseEntity<Void>> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve().toBodilessEntity();

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
					assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
					assertThat(entity.getHeaders().getContentLength()).isEqualTo(31);
					assertThat(entity.getBody()).isNull();
				})
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void exchangeForJsonArrayAsResponseEntity(ClientHttpConnector connector) {
		startServer(connector);

		String content = "[{\"bar\":\"bar1\",\"foo\":\"foo1\"}, {\"bar\":\"bar2\",\"foo\":\"foo2\"}]";
		prepareResponse(response -> response
				.setHeader("Content-Type", "application/json").setBody(content));

		Mono<ResponseEntity<List<Pojo>>> result = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve().toEntityList(Pojo.class);

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
					assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
					assertThat(entity.getHeaders().getContentLength()).isEqualTo(58);
					Pojo pojo1 = new Pojo("foo1", "bar1");
					Pojo pojo2 = new Pojo("foo2", "bar2");
					assertThat(entity.getBody()).isEqualTo(Arrays.asList(pojo1, pojo2));
				})
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getPath()).isEqualTo("/json");
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
		});
	}

	@ParameterizedWebClientTest
	void exchangeForEmptyBodyAsVoidEntity(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setHeader("Content-Length", "0").setBody(""));

		Mono<ResponseEntity<Void>> result = this.webClient.get()
				.uri("/noContent")
				.retrieve().toBodilessEntity();

		StepVerifier.create(result)
				.assertNext(r -> assertThat(r.getStatusCode().is2xxSuccessful()).isTrue())
				.expectComplete().verify(Duration.ofSeconds(3));
	}

	@ParameterizedWebClientTest
	void exchangeFor404(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setResponseCode(404)
				.setHeader("Content-Type", "text/plain").setBody("Not Found"));

		Mono<ResponseEntity<Void>> result = this.webClient.get().uri("/greeting")
				.exchangeToMono(ClientResponse::toBodilessEntity);

		StepVerifier.create(result)
				.consumeNextWith(entity -> assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/greeting");
		});
	}

	@ParameterizedWebClientTest
	void exchangeForUnknownStatusCode(ClientHttpConnector connector) {
		startServer(connector);

		int errorStatus = 555;
		assertThat((Object) HttpStatus.resolve(errorStatus)).isNull();
		String errorMessage = "Something went wrong";
		prepareResponse(response -> response.setResponseCode(errorStatus)
				.setHeader("Content-Type", "text/plain").setBody(errorMessage));

		Mono<ResponseEntity<Void>> result = this.webClient.get()
				.uri("/unknownPage")
				.exchangeToMono(ClientResponse::toBodilessEntity);

		StepVerifier.create(result)
				.consumeNextWith(entity -> assertThat(entity.getStatusCodeValue()).isEqualTo(555))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> {
			assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("*/*");
			assertThat(request.getPath()).isEqualTo("/unknownPage");
		});
	}

	@ParameterizedWebClientTest
	void filter(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
				.setBody("Hello Spring!"));

		WebClient filteredClient = this.webClient.mutate()
				.filter((request, next) -> {
					ClientRequest filteredRequest =
							ClientRequest.from(request).header("foo", "bar").build();
					return next.exchange(filteredRequest);
				})
				.build();

		Mono<String> result = filteredClient.get()
				.uri("/greeting")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		expectRequestCount(1);
		expectRequest(request -> assertThat(request.getHeader("foo")).isEqualTo("bar"));
	}

	@ParameterizedWebClientTest
	void filterForErrorHandling(ClientHttpConnector connector) {
		startServer(connector);

		ExchangeFilterFunction filter = ExchangeFilterFunction.ofResponseProcessor(
				clientResponse -> {
					List<String> headerValues = clientResponse.headers().header("Foo");
					return headerValues.isEmpty() ? Mono.error(
							new MyException("Response does not contain Foo header")) :
							Mono.just(clientResponse);
				}
		);

		WebClient filteredClient = this.webClient.mutate().filter(filter).build();

		// header not present
		prepareResponse(response -> response
				.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

		Mono<String> result = filteredClient.get()
				.uri("/greeting")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(MyException.class).verify(Duration.ofSeconds(3));

		// header present

		prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
				.setHeader("Foo", "Bar")
				.setBody("Hello Spring!"));

		result = filteredClient.get()
				.uri("/greeting")
				.retrieve().bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("Hello Spring!")
				.expectComplete().verify(Duration.ofSeconds(3));

		expectRequestCount(2);
	}

	@ParameterizedWebClientTest
	void exchangeResponseCookies(ClientHttpConnector connector) {
		startServer(connector);

		prepareResponse(response -> response
				.setHeader("Content-Type", "text/plain")
				.addHeader("Set-Cookie", "testkey1=testvalue1;")
				.addHeader("Set-Cookie", "testkey2=testvalue2; Max-Age=42; HttpOnly; SameSite=Lax; Secure")
				.setBody("test"));

		this.webClient.get()
				.uri("/test")
				.exchangeToMono(response -> {
					assertThat(response.cookies()).containsOnlyKeys("testkey1", "testkey2");

					ResponseCookie cookie1 = response.cookies().get("testkey1").get(0);
					assertThat(cookie1.getValue()).isEqualTo("testvalue1");
					assertThat(cookie1.isSecure()).isFalse();
					assertThat(cookie1.isHttpOnly()).isFalse();
					assertThat(cookie1.getMaxAge().getSeconds()).isEqualTo(-1);

					ResponseCookie cookie2 = response.cookies().get("testkey2").get(0);
					assertThat(cookie2.getValue()).isEqualTo("testvalue2");
					assertThat(cookie2.isSecure()).isTrue();
					assertThat(cookie2.isHttpOnly()).isTrue();
					assertThat(cookie2.getSameSite()).isEqualTo("Lax");
					assertThat(cookie2.getMaxAge().getSeconds()).isEqualTo(42);

					return response.releaseBody();
				})
				.block(Duration.ofSeconds(3));

		expectRequestCount(1);
	}

	@ParameterizedWebClientTest
	void invalidDomain(ClientHttpConnector connector) {
		startServer(connector);

		String url = "http://example.invalid";
		Mono<Void> result = this.webClient.get().uri(url).retrieve().bodyToMono(Void.class);

		StepVerifier.create(result)
				.expectErrorSatisfies(throwable -> {
					assertThat(throwable).isInstanceOf(WebClientRequestException.class);
					WebClientRequestException ex = (WebClientRequestException) throwable;
					assertThat(ex.getMethod()).isEqualTo(HttpMethod.GET);
					assertThat(ex.getUri()).isEqualTo(URI.create(url));
				})
				.verify();
	}

	@ParameterizedWebClientTest
	void malformedResponseChunksOnBodilessEntity(ClientHttpConnector connector) {
		Mono<?> result = doMalformedChunkedResponseTest(connector, ResponseSpec::toBodilessEntity);
		StepVerifier.create(result)
				.expectErrorSatisfies(throwable -> {
					assertThat(throwable).isInstanceOf(WebClientException.class);
					WebClientException ex = (WebClientException) throwable;
					assertThat(ex.getCause()).isInstanceOf(IOException.class);
				})
				.verify(Duration.ofSeconds(3));
	}

	@ParameterizedWebClientTest
	void malformedResponseChunksOnEntityWithBody(ClientHttpConnector connector) {
		Mono<?> result = doMalformedChunkedResponseTest(connector, spec -> spec.toEntity(String.class));
		StepVerifier.create(result)
				.expectErrorSatisfies(throwable -> {
					assertThat(throwable).isInstanceOf(WebClientException.class);
					WebClientException ex = (WebClientException) throwable;
					assertThat(ex.getCause()).isInstanceOf(IOException.class);
				})
				.verify(Duration.ofSeconds(3));
	}

	private <T> Mono<T> doMalformedChunkedResponseTest(
			ClientHttpConnector connector, Function<ResponseSpec, Mono<T>> handler) {

		@SuppressWarnings("deprecation")
		int port = org.springframework.util.SocketUtils.findAvailableTcpPort();

		Thread serverThread = new Thread(() -> {
			// No way to simulate a malformed chunked response through MockWebServer.
			try (ServerSocket serverSocket = new ServerSocket(port)) {
				Socket socket = serverSocket.accept();
				InputStream is = socket.getInputStream();

				//noinspection ResultOfMethodCallIgnored
				is.read(new byte[4096]);

				OutputStream os = socket.getOutputStream();
				os.write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
				os.write("Transfer-Encoding: chunked\r\n".getBytes(StandardCharsets.UTF_8));
				os.write("\r\n".getBytes(StandardCharsets.UTF_8));
				os.write("lskdu018973t09sylgasjkfg1][]'./.sdlv".getBytes(StandardCharsets.UTF_8));
				socket.close();
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});

		serverThread.start();

		WebClient client = WebClient.builder()
				.clientConnector(connector)
				.baseUrl("http://localhost:" + port)
				.build();

		return handler.apply(client.post().retrieve());
	}

	private void prepareResponse(Consumer<MockResponse> consumer) {
		MockResponse response = new MockResponse();
		consumer.accept(response);
		this.server.enqueue(response);
	}

	private void expectRequest(Consumer<RecordedRequest> consumer) {
		try {
			consumer.accept(this.server.takeRequest());
		}
		catch (InterruptedException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void expectRequestCount(int count) {
		assertThat(this.server.getRequestCount()).isEqualTo(count);
	}


	@SuppressWarnings("serial")
	private static class MyException extends RuntimeException {

		MyException(String message) {
			super(message);
		}
	}


	static class ValueContainer<T> {

		private T containerValue;


		public T getContainerValue() {
			return containerValue;
		}

		public void setContainerValue(T containerValue) {
			this.containerValue = containerValue;
		}
	}
}
