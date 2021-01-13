/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.function.RequestPredicates.HEAD;

/**
 * Unit tests for {@link RouterFunctionBuilder}.
 *
 * @author Arjen Poutsma
 */
class RouterFunctionBuilderTests {

	@Test
	void route() {
		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.GET("/foo", request -> ServerResponse.ok().build())
				.POST("/", RequestPredicates.contentType(MediaType.TEXT_PLAIN),
						request -> ServerResponse.noContent().build())
				.route(HEAD("/foo"), request -> ServerResponse.accepted().build())
				.build();

		ServerRequest getFooRequest = initRequest("GET", "/foo");

		Optional<HttpStatus> responseStatus = route.route(getFooRequest)
				.map(handlerFunction -> handle(handlerFunction, getFooRequest))
				.map(ServerResponse::statusCode);
		assertThat(responseStatus).contains(HttpStatus.OK);

		ServerRequest headFooRequest = initRequest("HEAD", "/foo");

		responseStatus = route.route(headFooRequest)
				.map(handlerFunction -> handle(handlerFunction, getFooRequest))
				.map(ServerResponse::statusCode);
		assertThat(responseStatus).contains(HttpStatus.ACCEPTED);

		ServerRequest barRequest = initRequest("POST", "/", req -> req.setContentType("text/plain"));

		responseStatus = route.route(barRequest)
				.map(handlerFunction -> handle(handlerFunction, barRequest))
				.map(ServerResponse::statusCode);
		assertThat(responseStatus).contains(HttpStatus.NO_CONTENT);

		ServerRequest invalidRequest = initRequest("POST", "/");

		responseStatus = route.route(invalidRequest)
				.map(handlerFunction -> handle(handlerFunction, invalidRequest))
				.map(ServerResponse::statusCode);

		assertThat(responseStatus).isEmpty();
	}

	private static ServerResponse handle(HandlerFunction<ServerResponse> handlerFunction,
			ServerRequest request) {
		try {
			return handlerFunction.handle(request);
		}
		catch (Exception ex) {
			throw new AssertionError(ex.getMessage(), ex);
		}
	}

	@Test
	void resources() {
		Resource resource = new ClassPathResource("/org/springframework/web/servlet/function/");
		assertThat(resource.exists()).isTrue();

		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.resources("/resources/**", resource)
				.build();

		ServerRequest resourceRequest = initRequest("GET", "/resources/response.txt");

		Optional<HttpStatus> responseStatus = route.route(resourceRequest)
				.map(handlerFunction -> handle(handlerFunction, resourceRequest))
				.map(ServerResponse::statusCode);
		assertThat(responseStatus).contains(HttpStatus.OK);

		ServerRequest invalidRequest = initRequest("POST", "/resources/foo.txt");

		responseStatus = route.route(invalidRequest)
				.map(handlerFunction -> handle(handlerFunction, invalidRequest))
				.map(ServerResponse::statusCode);
		assertThat(responseStatus).isEmpty();
	}

	@Test
	void nest() {
		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.path("/foo", builder ->
						builder.path("/bar",
								() -> RouterFunctions.route()
										.GET("/baz", request -> ServerResponse.ok().build())
										.build()))
				.build();

		ServerRequest fooRequest = initRequest("GET", "/foo/bar/baz");

		Optional<HttpStatus> responseStatus = route.route(fooRequest)
				.map(handlerFunction -> handle(handlerFunction, fooRequest))
				.map(ServerResponse::statusCode);
		assertThat(responseStatus).contains(HttpStatus.OK);
	}

	@Test
	void filters() {
		AtomicInteger filterCount = new AtomicInteger();

		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.GET("/foo", request -> ServerResponse.ok().build())
				.GET("/bar", request -> {
					throw new IllegalStateException();
				})
				.before(request -> {
					int count = filterCount.getAndIncrement();
					assertThat(count).isEqualTo(0);
					return request;
				})
				.after((request, response) -> {
					int count = filterCount.getAndIncrement();
					assertThat(count).isEqualTo(3);
					return response;
				})
				.filter((request, next) -> {
					int count = filterCount.getAndIncrement();
					assertThat(count).isEqualTo(1);
					ServerResponse responseMono = next.handle(request);
					count = filterCount.getAndIncrement();
					assertThat(count).isEqualTo(2);
					return responseMono;
				})
				.onError(IllegalStateException.class,
						(e, request) -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
								.build())
				.build();

		ServerRequest fooRequest = initRequest("GET", "/foo");

		route.route(fooRequest)
				.map(handlerFunction -> handle(handlerFunction, fooRequest));
		assertThat(filterCount.get()).isEqualTo(4);

		filterCount.set(0);

		ServerRequest barRequest = initRequest("GET", "/bar");

		Optional<HttpStatus> responseStatus = route.route(barRequest)
				.map(handlerFunction -> handle(handlerFunction, barRequest))
				.map(ServerResponse::statusCode);
		assertThat(responseStatus).contains(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	public void multipleOnErrors() {
		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.GET("/error", request -> {
					throw new IOException();
				})
				.onError(IOException.class, (t, r) -> ServerResponse.status(200).build())
				.onError(Exception.class, (t, r) -> ServerResponse.status(201).build())
				.build();

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/error");
		ServerRequest serverRequest = new DefaultServerRequest(servletRequest, emptyList());

		Optional<HttpStatus> responseStatus = route.route(serverRequest)
				.map(handlerFunction -> handle(handlerFunction, serverRequest))
				.map(ServerResponse::statusCode);
		assertThat(responseStatus).contains(HttpStatus.OK);

	}



	private ServerRequest initRequest(String httpMethod, String requestUri) {
		return initRequest(httpMethod, requestUri, null);
	}

	private ServerRequest initRequest(
			String httpMethod, String requestUri, @Nullable Consumer<MockHttpServletRequest> consumer) {

		return new DefaultServerRequest(
				PathPatternsTestUtils.initRequest(httpMethod, null, requestUri, true, consumer), emptyList());
	}

	@Test
	public void attributes() {
		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.GET("/atts/1", request -> ServerResponse.ok().build())
				.withAttribute("foo", "bar")
				.withAttribute("baz", "qux")
				.GET("/atts/2", request -> ServerResponse.ok().build())
				.withAttributes(atts -> {
					atts.put("foo", "bar");
					atts.put("baz", "qux");
				})
				.build();

		AttributesTestVisitor visitor = new AttributesTestVisitor();
		route.accept(visitor);
		assertThat(visitor.visitCount()).isEqualTo(2);
	}


}
