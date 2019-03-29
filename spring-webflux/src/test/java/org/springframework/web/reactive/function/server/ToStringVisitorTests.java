/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.junit.Assert.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.methods;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RequestPredicates.pathExtension;
import static org.springframework.web.reactive.function.server.RequestPredicates.queryParam;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 */
public class ToStringVisitorTests {

	@Test
	public void nested() {
		HandlerFunction<ServerResponse> handler = new SimpleHandlerFunction();
		RouterFunction<ServerResponse> routerFunction = route()
				.path("/foo", builder -> {
					builder.path("/bar", () -> route()
							.GET("/baz", handler)
							.build());
				})
				.build();

		ToStringVisitor visitor = new ToStringVisitor();
		routerFunction.accept(visitor);
		String result = visitor.toString();

		String expected = "/foo => {\n" +
				" /bar => {\n" +
				"  (GET && /baz) -> \n" +
				" }\n" +
				"}";
		assertEquals(expected, result);
	}

	@Test
	public void predicates() {
		testPredicate(methods(HttpMethod.GET), "GET");
		testPredicate(methods(HttpMethod.GET, HttpMethod.POST), "[GET, POST]");

		testPredicate(path("/foo"), "/foo");

		testPredicate(pathExtension("foo"), "*.foo");

		testPredicate(contentType(MediaType.APPLICATION_JSON), "Content-Type: application/json");
		testPredicate(contentType(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN), "Content-Type: [application/json, text/plain]");

		testPredicate(accept(MediaType.APPLICATION_JSON), "Accept: application/json");

		testPredicate(queryParam("foo", "bar"), "?foo == bar");

		testPredicate(method(HttpMethod.GET).and(path("/foo")), "(GET && /foo)");

		testPredicate(method(HttpMethod.GET).or(path("/foo")), "(GET || /foo)");

		testPredicate(method(HttpMethod.GET).negate(), "!(GET)");

		testPredicate(GET("/foo")
				.or(contentType(MediaType.TEXT_PLAIN))
				.and(accept(MediaType.APPLICATION_JSON).negate()),
				"(((GET && /foo) || Content-Type: text/plain) && !(Accept: application/json))");
	}

	private void testPredicate(RequestPredicate predicate, String expected) {
		ToStringVisitor visitor = new ToStringVisitor();
		predicate.accept(visitor);
		String result = visitor.toString();

		assertEquals(expected, result);
	}


	private static class SimpleHandlerFunction implements HandlerFunction<ServerResponse> {

		@Override
		public Mono<ServerResponse> handle(ServerRequest request) {
			return ServerResponse.ok().build();
		}

		@Override
		public String toString() {
			return "";
		}
	}

}
