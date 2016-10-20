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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.codec.BodyInserters.fromObject;

/**
 * @author Arjen Poutsma
 */
@SuppressWarnings("unchecked")
public class RouterFunctionTests {

	@Test
	public void andSame() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<Void> routerFunction1 = request -> Optional.empty();
		RouterFunction<Void> routerFunction2 = request -> Optional.of(handlerFunction);

		RouterFunction<Void> result = routerFunction1.andSame(routerFunction2);
		assertNotNull(result);

		MockServerRequest request = MockServerRequest.builder().build();
		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		assertEquals(handlerFunction, resultHandlerFunction.get());
	}

	@Test
	public void and() throws Exception {
		HandlerFunction<String> handlerFunction = request -> ServerResponse.ok().body(fromObject("42"));
		RouterFunction<Void> routerFunction1 = request -> Optional.empty();
		RouterFunction<String> routerFunction2 = request -> Optional.of(handlerFunction);

		RouterFunction<?> result = routerFunction1.and(routerFunction2);
		assertNotNull(result);

		MockServerRequest request = MockServerRequest.builder().build();
		Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		assertEquals(handlerFunction, resultHandlerFunction.get());
	}

	@Test
	public void filter() throws Exception {
		HandlerFunction<String> handlerFunction = request -> ServerResponse.ok().body(fromObject("42"));
		RouterFunction<String> routerFunction = request -> Optional.of(handlerFunction);

		HandlerFilterFunction<String, Integer> filterFunction = (request, next) -> {
			ServerResponse<String> response = next.handle(request);
			int i = Integer.parseInt(response.body());
			return ServerResponse.ok().body(fromObject(i));
		};
		RouterFunction<Integer> result = routerFunction.filter(filterFunction);
		assertNotNull(result);

		MockServerRequest request = MockServerRequest.builder().build();
		Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		ServerResponse<?> resultResponse = resultHandlerFunction.get().handle(request);
		assertEquals(42, resultResponse.body());
	}

}