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

package org.springframework.web.reactive;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for {@link DispatcherHandler}.
 * @author Rossen Stoyanchev
 */
public class DispatcherHandlerTests {

	private static final MethodParameter RETURN_TYPE =
			ResolvableMethod.on(DispatcherHandler.class).named("handle").resolveReturnType();


	@Test
	void handlerMappingOrder() {
		HandlerMapping hm1 = mock(HandlerMapping.class, withSettings().extraInterfaces(Ordered.class));
		HandlerMapping hm2 = mock(HandlerMapping.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) hm1).getOrder()).willReturn(1);
		given(((Ordered) hm2).getOrder()).willReturn(2);
		given((hm1).getHandler(any())).willReturn(Mono.just((Supplier<String>) () -> "1"));
		given((hm2).getHandler(any())).willReturn(Mono.just((Supplier<String>) () -> "2"));

		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBean("b2", HandlerMapping.class, () -> hm2);
		context.registerBean("b1", HandlerMapping.class, () -> hm1);
		context.registerBean(HandlerAdapter.class, SupplierHandlerAdapter::new);
		context.registerBean(HandlerResultHandler.class, StringHandlerResultHandler::new);
		context.refresh();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		new DispatcherHandler(context).handle(exchange).block(Duration.ofSeconds(0));

		assertThat(exchange.getResponse().getBodyAsString().block(Duration.ofSeconds(5))).isEqualTo("1");
	}

	@Test
	void preFlightRequest() {
		WebHandler webHandler = mock(WebHandler.class);
		HandlerMapping handlerMapping = mock(HandlerMapping.class);
		given((handlerMapping).getHandler(any())).willReturn(Mono.just(webHandler));

		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBean("handlerMapping", HandlerMapping.class, () -> handlerMapping);
		context.registerBean(HandlerAdapter.class, SimpleHandlerAdapter::new);
		context.registerBean(HandlerResultHandler.class, StringHandlerResultHandler::new);
		context.refresh();

		MockServerHttpRequest request = MockServerHttpRequest.options("/")
				.header(HttpHeaders.ORIGIN, "https://domain.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		new DispatcherHandler(context).handle(exchange).block(Duration.ofSeconds(0));

		verifyNoInteractions(webHandler);
	}

	@SuppressWarnings("unused")
	private void handle() {}


	private static class SupplierHandlerAdapter implements HandlerAdapter {

		@Override
		public boolean supports(Object handler) {
			return handler instanceof Supplier;
		}

		@Override
		public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
			return Mono.just(new HandlerResult(handler, ((Supplier<?>) handler).get(), RETURN_TYPE));
		}
	}


	private static class StringHandlerResultHandler implements HandlerResultHandler {

		@Override
		public boolean supports(HandlerResult result) {
			Object value = result.getReturnValue();
			return value != null && String.class.equals(value.getClass());
		}

		@Override
		public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
			Object returnValue = result.getReturnValue();
			if (returnValue == null) {
				return Mono.empty();
			}
			byte[] bytes = ((String) returnValue).getBytes(StandardCharsets.UTF_8);
			DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
			return exchange.getResponse().writeWith(Mono.just(dataBuffer));
		}
	}

}
