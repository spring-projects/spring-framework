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

import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.all;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 */
public class NestedRouteIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private final RestTemplate restTemplate = new RestTemplate();


	@Override
	protected RouterFunction<?> routerFunction() {
		NestedHandler nestedHandler = new NestedHandler();
		return nest(path("/foo/"),
					route(GET("/bar"), nestedHandler::pattern)
					.andRoute(GET("/baz"), nestedHandler::pattern))
				.andNest(GET("/{foo}"),
					route(GET("/bar"), nestedHandler::variables).and(
					nest(GET("/{bar}"),
								route(GET("/{baz}"), nestedHandler::variables))))
				.andRoute(path("/{qux}/quux").and(method(HttpMethod.GET)), nestedHandler::variables)
				.andRoute(all(), nestedHandler::variables);
	}


	@Test
	public void bar() {
		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/foo/bar", String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/foo/bar", result.getBody());
	}

	@Test
	public void baz() {
		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/foo/baz", String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/foo/baz", result.getBody());
	}

	@Test
	public void variables() {
		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/1/2/3", String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/{foo}/{bar}/{baz}\n{foo=1, bar=2, baz=3}", result.getBody());
	}

	// SPR-16868
	@Test
	public void parentVariables() {
		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/1/bar", String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/{foo}/bar\n{foo=1}", result.getBody());

	}

	// SPR 16692
	@Test
	public void removeFailedNestedPathVariables() {
		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/qux/quux", String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/{qux}/quux\n{qux=qux}", result.getBody());

	}

	// SPR 17210
	@Test
	public void removeFailedPathVariablesAnd() {
		ResponseEntity<String> result =
				restTemplate.postForEntity("http://localhost:" + port + "/qux/quux", "", String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("{}", result.getBody());

	}


	private static class NestedHandler {

		public Mono<ServerResponse> pattern(ServerRequest request) {
			String pattern = matchingPattern(request);
			return ServerResponse.ok().syncBody(pattern);
		}

		@SuppressWarnings("unchecked")
		public Mono<ServerResponse> variables(ServerRequest request) {
			Map<String, String> pathVariables = request.pathVariables();
			Map<String, String> attributePathVariables =
					(Map<String, String>) request.attributes().get(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			assertTrue( (pathVariables.equals(attributePathVariables))
					|| (pathVariables.isEmpty() && (attributePathVariables == null)));

			String pattern = matchingPattern(request);
			Flux<String> responseBody;
			if (!pattern.isEmpty()) {
				responseBody = Flux.just(pattern, "\n", pathVariables.toString());
			} else {
				responseBody = Flux.just(pathVariables.toString());
			}
			return ServerResponse.ok().body(responseBody, String.class);
		}

		private String matchingPattern(ServerRequest request) {
			return (String) request.attributes().getOrDefault(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, "");
		}

	}

}
