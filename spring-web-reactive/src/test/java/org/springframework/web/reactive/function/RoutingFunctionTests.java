/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
@SuppressWarnings("unchecked")
public class RoutingFunctionTests {

	@Test
	public void and() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> Response.ok().build();
		RoutingFunction<Void> routingFunction1 = request -> Optional.empty();
		RoutingFunction<Void> routingFunction2 = request -> Optional.of(handlerFunction);

		RoutingFunction<Void> result = routingFunction1.and(routingFunction2);
		assertNotNull(result);

		MockRequest request = MockRequest.builder().build();
		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		assertEquals(handlerFunction, resultHandlerFunction.get());
	}

	@Test
	public void andOther() throws Exception {
		HandlerFunction<String> handlerFunction = request -> Response.ok().body("42");
		RoutingFunction<Void> routingFunction1 = request -> Optional.empty();
		RoutingFunction<String> routingFunction2 = request -> Optional.of(handlerFunction);

		RoutingFunction<?> result = routingFunction1.andOther(routingFunction2);
		assertNotNull(result);

		MockRequest request = MockRequest.builder().build();
		Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		assertEquals(handlerFunction, resultHandlerFunction.get());
	}

	@Test
	public void filter() throws Exception {
		HandlerFunction<String> handlerFunction = request -> Response.ok().body("42");
		RoutingFunction<String> routingFunction = request -> Optional.of(handlerFunction);

		FilterFunction<String, Integer> filterFunction = (request, next) -> {
			Response<String> response = next.handle(request);
			int i = Integer.parseInt(response.body());
			return Response.ok().body(i);
		};
		RoutingFunction<Integer> result = routingFunction.filter(filterFunction);
		assertNotNull(result);

		MockRequest request = MockRequest.builder().build();
		Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		Response<?> resultResponse = resultHandlerFunction.get().handle(request);
		assertEquals(42, resultResponse.body());
	}

}