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

package org.springframework.web.reactive.function.server;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.pathPrefix;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 */
public class NestedRouteIntegrationTests
		extends AbstractRouterFunctionIntegrationTests {

	private RestTemplate restTemplate;

	@Before
	public void createRestTemplate() {
		this.restTemplate = new RestTemplate();
	}

	@Override
	protected RouterFunction<?> routerFunction() {
		NestedHandler nestedHandler = new NestedHandler();
		return nest(pathPrefix("/foo"),
				route(GET("/bar"), nestedHandler::bar)
				.andRoute(GET("/baz"), nestedHandler::baz));
	}


	@Test
	public void bar() throws Exception {
		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/foo/bar", String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("bar", result.getBody());
	}

	@Test
	@Ignore
	public void baz() throws Exception {
		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/foo/baz", String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("baz", result.getBody());
	}

	private static class NestedHandler {

		public Mono<ServerResponse> bar(ServerRequest request) {
			return ServerResponse.ok().body(fromObject("bar"));
		}

		public Mono<ServerResponse> baz(ServerRequest request) {
			return ServerResponse.ok().body(fromObject("baz"));
		}

	}

}
