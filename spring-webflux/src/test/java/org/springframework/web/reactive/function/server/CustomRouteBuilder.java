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

package org.springframework.web.reactive.function.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * @author Arjen Poutsma
 */
public class CustomRouteBuilder {

	public static final String OPERATION_ATTRIBUTE = CustomRouteBuilder.class.getName() + ".operation";

	private final RouterFunctions.Builder delegate = RouterFunctions.route();


	private CustomRouteBuilder() {
	}

	public static CustomRouteBuilder route() {
		return new CustomRouteBuilder();
	}

	public CustomRouteBuilder GET(String pattern, HandlerFunction<ServerResponse> handlerFunction,
			Consumer<OperationBuilder> operationsConsumer) {

		OperationBuilder builder = new OperationBuilder();
		operationsConsumer.accept(builder);

		this.delegate.GET(pattern, handlerFunction)
				.withAttribute(OPERATION_ATTRIBUTE, builder.operation);

		return this;
	}

	public RouterFunction<ServerResponse> build() {
		return this.delegate.build();
	}

	public static void main(String[] args) {
		RouterFunction<ServerResponse> routerFunction =
				route()
						.GET("/foo", request -> ServerResponse.ok().build(), ops -> ops
								.parameter("key1", "My key1 description")
								.parameter("key1", "My key1 description")
								.response(200, "This is normal response description")
								.response(404, "This is response description")
						)
						.build();

		AttributesVisitor visitor = new AttributesVisitor();
		routerFunction.accept(visitor);
	}


	public static class OperationBuilder {

		private final Operation operation = new Operation();

		public OperationBuilder parameter(String name, String description) {
			this.operation.parameter(name, description);
			return this;
		}

		public OperationBuilder response(int statusCode, String description) {
			this.operation.response(statusCode, description);
			return this;
		}

	}


	static class Operation {

		private final Map<String, String> parameters = new LinkedHashMap<>();

		private final Map<Integer, String > responses = new LinkedHashMap<>();

		public void parameter(String name, String description) {
			this.parameters.put(name, description);
		}

		public void response(int status, String description) {
			this.responses.put(status, description);
		}

		@Override
		public String toString() {
			return "parameters=" + parameters +
					", responses=" + responses;
		}
	}

	static class AttributesVisitor implements RouterFunctions.Visitor {

		@Nullable
		private Map<String, Object> attributes;

		@Override
		public void attributes(Map<String, Object> attributes) {
			this.attributes = attributes;
		}

		@Override
		public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
			System.out.printf("Route predicate %s->%s%nhas attributes %s", predicate, handlerFunction, this.attributes);
			this.attributes = null;
		}

		@Override
		public void startNested(RequestPredicate predicate) {
			// TODO
		}

		@Override
		public void endNested(RequestPredicate predicate) {
			// TODO

		}

		@Override
		public void resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
			// TODO

		}

		@Override
		public void unknown(RouterFunction<?> routerFunction) {
			// TODO

		}
	}


}
