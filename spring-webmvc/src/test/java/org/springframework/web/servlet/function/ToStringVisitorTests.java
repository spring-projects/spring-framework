/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RequestPredicates.contentType;
import static org.springframework.web.servlet.function.RequestPredicates.method;
import static org.springframework.web.servlet.function.RequestPredicates.methods;
import static org.springframework.web.servlet.function.RequestPredicates.param;
import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 */
class ToStringVisitorTests {

	@Test
	void nested() {
		HandlerFunction<ServerResponse> handler = new SimpleHandlerFunction();
		RouterFunction<ServerResponse> routerFunction = route()
				.path("/foo", builder ->
					builder.path("/bar", () -> route()
							.GET("/baz", handler)
							.build())
				)
				.build();

		ToStringVisitor visitor = new ToStringVisitor();
		routerFunction.accept(visitor);
		String result = visitor.toString();

		String expected = """
				/foo => {
					/bar => {
						(GET && /baz) ->\s
					}
				}""".replace('\t', ' ');
		assertThat(result).isEqualTo(expected);
	}

	@SuppressWarnings("removal")
	@Test
	void predicates() {
		testPredicate(methods(HttpMethod.GET), "GET");
		testPredicate(methods(HttpMethod.GET, HttpMethod.POST), "[GET, POST]");

		testPredicate(path("/foo"), "/foo");

		testPredicate(contentType(MediaType.APPLICATION_JSON), "Content-Type: application/json");

		ToStringVisitor visitor = new ToStringVisitor();
		contentType(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN).accept(visitor);
		assertThat(visitor.toString()).matches("Content-Type: \\[.+, .+\\]").contains("application/json", "text/plain");

		testPredicate(accept(MediaType.APPLICATION_JSON), "Accept: application/json");

		testPredicate(param("foo", "bar"), "?foo == bar");

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
		assertThat(visitor).asString().isEqualTo(expected);
	}


	private static class SimpleHandlerFunction implements HandlerFunction<ServerResponse> {

		@Override
		public ServerResponse handle(ServerRequest request) {
			return ServerResponse.ok().build();
		}

		@Override
		public String toString() {
			return "";
		}
	}

}
