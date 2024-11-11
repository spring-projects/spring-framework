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

package org.springframework.web.reactive.function.server;

import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.util.pattern.PathPattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.all;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 */
class NestedRouteIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private final RestTemplate restTemplate = new RestTemplate();


	@Override
	protected RouterFunction<?> routerFunction() {
		NestedHandler nestedHandler = new NestedHandler();
		return nest(path("/foo/"),
					route(GET("/bar"), nestedHandler::pattern)
					.andRoute(GET("/baz"), nestedHandler::pattern))
				.andNest(GET("{foo}"),
					route(GET("/bar"), nestedHandler::variables).and(
					nest(GET("/{bar}"),
								route(GET("/{baz}"), nestedHandler::variables))))
				.andRoute(path("/{qux}/quux").and(method(HttpMethod.GET)), nestedHandler::variables)
				.andRoute(all(), nestedHandler::variables);
	}


	@ParameterizedHttpServerTest
	void bar(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/foo/bar", String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("/foo/bar");
	}

	@ParameterizedHttpServerTest
	void baz(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/foo/baz", String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("/foo/baz");
	}

	@ParameterizedHttpServerTest
	void variables(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/1/2/3", String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = result.getBody();
		assertThat(body).startsWith("/{foo}/{bar}/{baz}");
		assertThat(body).contains("foo=1");
		assertThat(body).contains("bar=2");
		assertThat(body).contains("baz=3");
	}

	// SPR-16868
	@ParameterizedHttpServerTest
	void parentVariables(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/1/bar", String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("/{foo}/bar\n{foo=1}");

	}

	// SPR 16692
	@ParameterizedHttpServerTest
	void removeFailedNestedPathVariables(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/qux/quux", String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("/{qux}/quux\n{qux=qux}");

	}

	// SPR 17210
	@ParameterizedHttpServerTest
	void removeFailedPathVariablesAnd(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> result =
				restTemplate.postForEntity("http://localhost:" + port + "/qux/quux", "", String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("{}");

	}


	private static class NestedHandler {

		public Mono<ServerResponse> pattern(ServerRequest request) {
			String pattern = matchingPattern(request).getPatternString();
			return ServerResponse.ok().bodyValue(pattern);
		}

		@SuppressWarnings("unchecked")
		public Mono<ServerResponse> variables(ServerRequest request) {
			Map<String, String> pathVariables = request.pathVariables();
			Map<String, String> attributePathVariables =
					(Map<String, String>) request.attributes().get(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			assertThat((pathVariables.equals(attributePathVariables))
						|| (pathVariables.isEmpty() && (attributePathVariables == null))).isTrue();

			PathPattern pathPattern = matchingPattern(request);
			String pattern = pathPattern != null ? pathPattern.getPatternString() : "";
			Flux<String> responseBody;
			if (!pattern.isEmpty()) {
				responseBody = Flux.just(pattern, "\n", pathVariables.toString());
			}
			else {
				responseBody = Flux.just(pathVariables.toString());
			}
			return ServerResponse.ok().body(responseBody, String.class);
		}

		@Nullable
		private PathPattern matchingPattern(ServerRequest request) {
			return (PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
		}

	}

}
