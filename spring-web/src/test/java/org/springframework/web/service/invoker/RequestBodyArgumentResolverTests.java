/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.service.invoker;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link RequestBodyArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class RequestBodyArgumentResolverTests {

	private final TestReactorExchangeAdapter client = new TestReactorExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	void stringBody() {
		String body = "bodyValue";
		this.service.execute(body);

		assertThat(getBodyValue()).isEqualTo(body);
		assertThat(getPublisherBody()).isNull();
	}

	@Test
	void monoBody() {
		Mono<String> bodyMono = Mono.just("bodyValue");
		this.service.executeMono(bodyMono);

		assertThat(getBodyValue()).isNull();
		assertThat(getPublisherBody()).isSameAs(bodyMono);
		assertThat(getBodyElementType()).isEqualTo(new ParameterizedTypeReference<String>() {});
	}

	@Test
	@SuppressWarnings("unchecked")
	void singleBody() {
		String bodyValue = "bodyValue";
		this.service.executeSingle(Single.just(bodyValue));

		assertThat(getBodyValue()).isNull();
		assertThat(getBodyElementType()).isEqualTo(new ParameterizedTypeReference<String>() {});

		Publisher<?> body = getPublisherBody();
		assertThat(body).isNotNull();
		assertThat(((Mono<String>) body).block()).isEqualTo(bodyValue);
	}

	@Test
	void monoVoid() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.service.executeMonoVoid(Mono.empty()))
				.withMessage("Async type for @RequestBody should produce value(s)");
	}

	@Test
	void completable() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.service.executeCompletable(Completable.complete()))
				.withMessage("Async type for @RequestBody should produce value(s)");
	}

	@Test
	void notRequestBody() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.service.executeNotRequestBody("value"))
				.withMessage("Could not resolve parameter [0] in " +
						"public abstract void org.springframework.web.service.invoker." +
						"RequestBodyArgumentResolverTests$Service.executeNotRequestBody(java.lang.String): " +
						"No suitable resolver");
	}

	@Test
	void ignoreNull() {
		this.service.execute(null);

		assertThat(getBodyValue()).isNull();
		assertThat(getPublisherBody()).isNull();

		this.service.executeMono(null);

		assertThat(getBodyValue()).isNull();
		assertThat(getPublisherBody()).isNull();
	}

	@Nullable
	private Object getBodyValue() {
		return getReactiveRequestValues().getBodyValue();
	}

	@Nullable
	private Publisher<?> getPublisherBody() {
		return getReactiveRequestValues().getBodyPublisher();
	}

	@Nullable
	private ParameterizedTypeReference<?> getBodyElementType() {
		return getReactiveRequestValues().getBodyPublisherElementType();
	}

	private ReactiveHttpRequestValues getReactiveRequestValues() {
		return (ReactiveHttpRequestValues) this.client.getRequestValues();
	}


	private interface Service {

		@GetExchange
		void execute(@RequestBody String body);

		@GetExchange
		void executeMono(@RequestBody Mono<String> body);

		@GetExchange
		void executeSingle(@RequestBody Single<String> body);

		@GetExchange
		void executeMonoVoid(@RequestBody Mono<Void> body);

		@GetExchange
		void executeCompletable(@RequestBody Completable body);

		@GetExchange
		void executeNotRequestBody(String body);
	}

}
