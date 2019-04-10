/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class RouterFunctionBuilderTests {

	@Test
	public void route() {
		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.GET("/foo", request -> ServerResponse.ok().build())
				.POST("/", RequestPredicates.contentType(MediaType.TEXT_PLAIN),
						request -> ServerResponse.noContent().build())
				.build();

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/foo");
		ServerRequest fooRequest = new DefaultServerRequest(servletRequest, emptyList());

		Optional<Integer> responseStatus = route.route(fooRequest)
				.map(handlerFunction -> handle(handlerFunction, fooRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);
		assertEquals(200, responseStatus.get().intValue());

		servletRequest = new MockHttpServletRequest("POST", "/");
		servletRequest.setContentType("text/plain");
		ServerRequest barRequest = new DefaultServerRequest(servletRequest, emptyList());

		responseStatus = route.route(barRequest)
				.map(handlerFunction -> handle(handlerFunction, barRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);
		assertEquals(204, responseStatus.get().intValue());

		servletRequest = new MockHttpServletRequest("POST", "/");
		ServerRequest invalidRequest = new DefaultServerRequest(servletRequest, emptyList());

		responseStatus = route.route(invalidRequest)
				.map(handlerFunction -> handle(handlerFunction, invalidRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);

		assertFalse(responseStatus.isPresent());

	}

	private static ServerResponse handle(HandlerFunction<ServerResponse> handlerFunction,
			ServerRequest request) {
		try {
			return handlerFunction.handle(request);
		}
		catch (Exception e) {
			fail(e.getMessage());
			return null;
		}
	}

	@Test
	public void resources() {
		Resource resource = new ClassPathResource("/org/springframework/web/servlet/function/");
		assertTrue(resource.exists());

		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.resources("/resources/**", resource)
				.build();

		MockHttpServletRequest servletRequest =
				new MockHttpServletRequest("GET", "/resources/response.txt");
		ServerRequest resourceRequest = new DefaultServerRequest(servletRequest, emptyList());

		Optional<Integer> responseStatus = route.route(resourceRequest)
				.map(handlerFunction -> handle(handlerFunction, resourceRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);
		assertEquals(200, responseStatus.get().intValue());

		servletRequest = new MockHttpServletRequest("POST", "/resources/foo.txt");
		ServerRequest invalidRequest = new DefaultServerRequest(servletRequest, emptyList());

		responseStatus = route.route(invalidRequest)
				.map(handlerFunction -> handle(handlerFunction, invalidRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);
		assertFalse(responseStatus.isPresent());
	}

	@Test
	public void nest() {
		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.path("/foo", builder ->
						builder.path("/bar",
								() -> RouterFunctions.route()
										.GET("/baz", request -> ServerResponse.ok().build())
										.build()))
				.build();

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/foo/bar/baz");
		ServerRequest fooRequest = new DefaultServerRequest(servletRequest, emptyList());

		Optional<Integer> responseStatus = route.route(fooRequest)
				.map(handlerFunction -> handle(handlerFunction, fooRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);
		assertEquals(200, responseStatus.get().intValue());
	}

	@Test
	public void filters() {
		AtomicInteger filterCount = new AtomicInteger();

		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.GET("/foo", request -> ServerResponse.ok().build())
				.GET("/bar", request -> {
					throw new IllegalStateException();
				})
				.before(request -> {
					int count = filterCount.getAndIncrement();
					assertEquals(0, count);
					return request;
				})
				.after((request, response) -> {
					int count = filterCount.getAndIncrement();
					assertEquals(3, count);
					return response;
				})
				.filter((request, next) -> {
					int count = filterCount.getAndIncrement();
					assertEquals(1, count);
					ServerResponse responseMono = next.handle(request);
					count = filterCount.getAndIncrement();
					assertEquals(2, count);
					return responseMono;
				})
				.onError(IllegalStateException.class,
						(e, request) -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
								.build())
				.build();

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/foo");
		ServerRequest fooRequest = new DefaultServerRequest(servletRequest, emptyList());

		Optional<ServerResponse> fooResponse = route.route(fooRequest)
				.map(handlerFunction -> handle(handlerFunction, fooRequest));
		assertEquals(4, filterCount.get());

		filterCount.set(0);

		servletRequest = new MockHttpServletRequest("GET", "/bar");
		ServerRequest barRequest = new DefaultServerRequest(servletRequest, emptyList());

		Optional<Integer> responseStatus = route.route(barRequest)
				.map(handlerFunction -> handle(handlerFunction, barRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);
		assertEquals(500, responseStatus.get().intValue());
	}

}
