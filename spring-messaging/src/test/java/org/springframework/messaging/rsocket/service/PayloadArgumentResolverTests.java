/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.rsocket.service;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.messaging.handler.annotation.Payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link PayloadArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class PayloadArgumentResolverTests extends RSocketServiceArgumentResolverTestSupport {

	@Override
	protected RSocketServiceArgumentResolver initResolver() {
		return new PayloadArgumentResolver(ReactiveAdapterRegistry.getSharedInstance(), false);
	}

	@Test
	void stringPayload() {
		String payload = "payloadValue";
		boolean resolved = execute(payload, initMethodParameter(Service.class, "execute", 0));

		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getPayloadValue()).isEqualTo(payload);
		assertThat(getRequestValues().getPayload()).isNull();
	}

	@Test
	void monoPayload() {
		Mono<String> payloadMono = Mono.just("payloadValue");
		boolean resolved = execute(payloadMono, initMethodParameter(Service.class, "executeMono", 0));

		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getPayloadValue()).isNull();
		assertThat(getRequestValues().getPayload()).isSameAs(payloadMono);
		assertThat(getRequestValues().getPayloadElementType()).isEqualTo(new ParameterizedTypeReference<String>() {});
	}

	@Test
	@SuppressWarnings("unchecked")
	void singlePayload() {
		boolean resolved = execute(Single.just("bodyValue"), initMethodParameter(Service.class, "executeSingle", 0));

		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getPayloadValue()).isNull();
		assertThat(getRequestValues().getPayloadElementType()).isEqualTo(new ParameterizedTypeReference<String>() {});

		Publisher<?> payload = getRequestValues().getPayload();
		assertThat(payload).isNotNull();
		assertThat(((Mono<String>) payload).block()).isEqualTo("bodyValue");
	}

	@Test
	void monoVoid() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> execute(Mono.empty(), initMethodParameter(Service.class, "executeMonoVoid", 0)))
				.withMessage("Async type for @Payload should produce value(s)");
	}

	@Test
	void completable() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> execute(Completable.complete(), initMethodParameter(Service.class, "executeCompletable", 0)))
				.withMessage("Async type for @Payload should produce value(s)");
	}

	@Test
	void notRequestBody() {
		MethodParameter parameter = initMethodParameter(Service.class, "executeNotAnnotated", 0);
		boolean resolved = execute("value", parameter);

		assertThat(resolved).isFalse();
	}

	@Test
	void ignoreNull() {
		boolean resolved = execute(null, initMethodParameter(Service.class, "execute", 0));

		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getPayloadValue()).isNull();
		assertThat(getRequestValues().getPayload()).isNull();
		assertThat(getRequestValues().getPayloadElementType()).isNull();
	}


	@SuppressWarnings("unused")
	private interface Service {

		void execute(@Payload String body);

		void executeMono(@Payload Mono<String> body);

		void executeSingle(@Payload Single<String> body);

		void executeMonoVoid(@Payload Mono<Void> body);

		void executeCompletable(@Payload Completable body);

		void executeNotAnnotated(String body);

	}

}
