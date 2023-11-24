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

package org.springframework.web.servlet.function;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class RouterFunctionsTests {

	private final ServerRequest request = new DefaultServerRequest(
			PathPatternsTestUtils.initRequest("GET", "", true), Collections.emptyList());


	@Test
	public void routeMatch() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

		RequestPredicate requestPredicate = mock();
		given(requestPredicate.test(request)).willReturn(true);

		RouterFunction<ServerResponse>
				result = RouterFunctions.route(requestPredicate, handlerFunction);
		assertThat(result).isNotNull();

		Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		assertThat(resultHandlerFunction).isPresent();
		assertThat(resultHandlerFunction).contains(handlerFunction);
	}

	@Test
	public void routeNoMatch() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

		RequestPredicate requestPredicate = mock();
		given(requestPredicate.test(request)).willReturn(false);

		RouterFunction<ServerResponse> result = RouterFunctions.route(requestPredicate, handlerFunction);
		assertThat(result).isNotNull();

		Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		assertThat(resultHandlerFunction).isNotPresent();
	}

	@Test
	public void nestMatch() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Optional.of(handlerFunction);

		RequestPredicate requestPredicate = mock();
		given(requestPredicate.nest(request)).willReturn(Optional.of(request));

		RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
		assertThat(result).isNotNull();

		Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		assertThat(resultHandlerFunction).isPresent();
		assertThat(resultHandlerFunction).contains(handlerFunction);
	}

	@Test
	public void nestNoMatch() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Optional.of(handlerFunction);

		RequestPredicate requestPredicate = mock();
		given(requestPredicate.nest(request)).willReturn(Optional.empty());

		RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
		assertThat(result).isNotNull();

		Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		assertThat(resultHandlerFunction).isNotPresent();
	}

	@Test
	public void nestPathVariable() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RequestPredicate requestPredicate = request -> request.pathVariable("foo").equals("bar");
		RouterFunction<ServerResponse> nestedFunction = RouterFunctions.route(requestPredicate, handlerFunction);

		RouterFunction<ServerResponse> result = RouterFunctions.nest(RequestPredicates.path("/{foo}"), nestedFunction);
		assertThat(result).isNotNull();

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/bar");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.emptyList());
		Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		assertThat(resultHandlerFunction).isPresent();
		assertThat(resultHandlerFunction).contains(handlerFunction);
	}

}
