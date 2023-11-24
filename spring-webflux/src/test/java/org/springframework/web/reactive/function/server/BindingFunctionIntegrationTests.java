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

package org.springframework.web.reactive.function.server;

import java.time.Duration;
import java.util.Objects;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 */
class BindingFunctionIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private WebClient webClient;


	@Override
	protected void startServer(HttpServer httpServer) throws Exception {
		super.startServer(httpServer);
		this.webClient = WebClient.create("http://localhost:" + this.port);
	}

	@Override
	protected RouterFunction<?> routerFunction() {
		return route()
				.GET("/constructor", request -> ServerResponse.ok().body(
						request.bind(ConstructorInjection.class).map(Objects::toString), String.class))
				.GET("/property", request -> ServerResponse.ok().body(
						request.bind(PropertyInjection.class).map(Objects::toString), String.class))
				.GET("/mixed", request -> ServerResponse.ok().body(
						request.bind(MixedInjection.class).map(Objects::toString), String.class))
				.GET("/customize", request -> ServerResponse.ok().body(
						request.bind(PropertyInjection.class, dataBinder -> dataBinder.setAllowedFields("foo")).map(Objects::toString), String.class))
				.GET("/error", request -> ServerResponse.ok().body(
						request.bind(ErrorInjection.class).map(Objects::toString), String.class))
				.build();
	}

	@ParameterizedHttpServerTest
	void bindToConstructor(HttpServer httpServer) throws Exception {

		startServer(httpServer);

		Mono<String> result = this.webClient.get()
				.uri("/constructor?foo=FOO&bar=BAR")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("FOO:BAR")
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void bindToProperties(HttpServer httpServer) throws Exception {

		startServer(httpServer);

		Mono<String> result = this.webClient.get()
				.uri("/property?foo=FOO&bar=BAR")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("FOO:BAR")
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void bindToMixed(HttpServer httpServer) throws Exception {

			startServer(httpServer);

			Mono<String> result = this.webClient.get()
					.uri("/mixed?foo=FOO&bar=BAR")
					.retrieve()
					.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("FOO:BAR")
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void bindCustomizer(HttpServer httpServer) throws Exception {

		startServer(httpServer);

		Mono<String> result = this.webClient.get()
				.uri("/customize?foo=FOO&bar=BAR")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectNext("FOO:null")
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void bindError(HttpServer httpServer) throws Exception {

		startServer(httpServer);

		Mono<String> result = this.webClient.get()
				.uri("/error?foo=FOO")
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.expectError(WebClientResponseException.InternalServerError.class)
				.verify(Duration.ofSeconds(5L));
	}


	@SuppressWarnings("unused")
	private static final class ConstructorInjection {

		private final String foo;

		private final String bar;

		public ConstructorInjection(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public String getFoo() {
			return this.foo;
		}

		public String getBar() {
			return this.bar;
		}

		@Override
		public String toString() {
			return this.foo + ":" + this.bar;
		}
	}

	@SuppressWarnings("unused")
	private static final class PropertyInjection {

		@Nullable
		private String foo;

		@Nullable
		private String bar;

		@Nullable
		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		@Nullable
		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public String toString() {
			return String.valueOf(this.foo) + ":" + String.valueOf(this.bar);
		}
	}

	@SuppressWarnings("unused")
	private static final class MixedInjection {

		private final String foo;

		@Nullable
		private String bar;

		public MixedInjection(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return this.foo;
		}

		@Nullable
		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public String toString() {
			return this.foo + ":" + String.valueOf(this.bar);
		}
	}

	@SuppressWarnings("unused")
	private static final class ErrorInjection {

		private int foo;

		public int getFoo() {
			return this.foo;
		}

		public void setFoo(int foo) {
			this.foo = foo;
		}
	}

}
