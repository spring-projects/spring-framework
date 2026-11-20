/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Arjen Poutsma
 */
class PathResourceLookupFunctionTests {

	@Test
	void normal() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/server/");

		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/resources/response.txt").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		Mono<Resource> result = function.apply(request);

		File expected = new ClassPathResource("response.txt", getClass()).getFile();
		StepVerifier.create(result)
				.expectNextMatches(resource -> {
					try {
						return expected.equals(resource.getFile());
					}
					catch (IOException ex) {
						return false;
					}
				})
				.expectComplete()
				.verify();
	}

	@Test
	void subPath() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/server/");

		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/resources/child/response.txt").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		Mono<Resource> result = function.apply(request);
		String path = "org/springframework/web/reactive/function/server/child/response.txt";
		File expected = new ClassPathResource(path).getFile();
		StepVerifier.create(result)
				.expectNextMatches(resource -> {
					try {
						return expected.equals(resource.getFile());
					}
					catch (IOException ex) {
						return false;
					}
				})
				.expectComplete()
				.verify();
	}

	@Test
	void notFound() {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/server/");

		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		Mono<Resource> result = function.apply(request);
		StepVerifier.create(result)
				.expectComplete()
				.verify();
	}

	@Test
	void composeResourceLookupFunction() {
		ClassPathResource defaultResource = new ClassPathResource("response.txt", getClass());

		Function<ServerRequest, Mono<Resource>> lookupFunction =
				new PathResourceLookupFunction("/resources/**",
						new ClassPathResource("org/springframework/web/reactive/function/server/"));

		Function<ServerRequest, Mono<Resource>> customLookupFunction =
				lookupFunction.andThen(resourceMono -> resourceMono
								.switchIfEmpty(Mono.just(defaultResource)));

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/resources/foo").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<Resource> result = customLookupFunction.apply(request);
		StepVerifier.create(result)
				.expectNextMatches(resource -> {
					try {
						return defaultResource.getFile().equals(resource.getFile());
					}
					catch (IOException ex) {
						return false;
					}
				})
				.expectComplete()
				.verify();
	}

	@Test
	@SuppressWarnings("removal")
	void withPathResource() {
		org.springframework.core.io.PathResource location = new org.springframework.core.io.PathResource("/static/");
		assertThatNoException().isThrownBy(() -> new PathResourceLookupFunction("/resources/**", location));
	}

	@Test
	void withFileSystemResource() {
		FileSystemResource location = new FileSystemResource("/static/");
		assertThatNoException().isThrownBy(() -> new PathResourceLookupFunction("/resources/**", location));
	}

}
