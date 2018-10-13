/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author Arjen Poutsma
 */
public class PathResourceLookupFunctionTests {

	@Test
	public void normal() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/server/");

		PathResourceLookupFunction
				function = new PathResourceLookupFunction("/resources/**", location);
		MockServerRequest request = MockServerRequest.builder()
				.uri(new URI("http://localhost/resources/response.txt"))
				.build();
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
	public void subPath() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/server/");

		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		MockServerRequest request = MockServerRequest.builder()
				.uri(new URI("http://localhost/resources/child/response.txt"))
				.build();
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
	public void notFound() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/server/");

		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		MockServerRequest request = MockServerRequest.builder()
				.uri(new URI("http://localhost/resources/foo"))
				.build();
		Mono<Resource> result = function.apply(request);
		StepVerifier.create(result)
				.expectComplete()
				.verify();
	}

	@Test
	public void composeResourceLookupFunction() throws Exception {
		ClassPathResource defaultResource = new ClassPathResource("response.txt", getClass());

		Function<ServerRequest, Mono<Resource>> lookupFunction =
				new PathResourceLookupFunction("/resources/**",
						new ClassPathResource("org/springframework/web/reactive/function/server/"));

		Function<ServerRequest, Mono<Resource>> customLookupFunction =
				lookupFunction.andThen(resourceMono -> resourceMono
								.switchIfEmpty(Mono.just(defaultResource)));

		MockServerRequest request = MockServerRequest.builder()
				.uri(new URI("http://localhost/resources/foo"))
				.build();

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

}
