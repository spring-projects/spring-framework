/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.jspecify.annotations.Nullable;
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
 * Tests for {@link PayloadArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("DataFlowIssue")
class PayloadArgumentResolverTests extends RSocketServiceArgumentResolverTestSupport {

	@Override
	protected RSocketServiceArgumentResolver initResolver() {
		return new PayloadArgumentResolver(ReactiveAdapterRegistry.getSharedInstance(), false);
	}

	@Test
	void stringPayload() {
		String payload = "payloadValue";
		boolean resolved = execute(payload, initMethodParameter(Service.class, "execute", 0));

		assertPayload(resolved, payload);
	}

	@Test
	void monoPayload() {
		Mono<String> payloadMono = Mono.just("payloadValue");
		boolean resolved = execute(payloadMono, initMethodParameter(Service.class, "executeMono", 0));

		assertPayloadMono(resolved, payloadMono);
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
	void notPayload() {
		MethodParameter parameter = initMethodParameter(Service.class, "executeNotAnnotated", 0);
		boolean resolved = execute("value", parameter);

		assertThat(resolved).isFalse();
	}

	@Test
	void nullPayload() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> execute(null, initMethodParameter(Service.class, "execute", 0)))
				.withMessage("Missing payload");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> execute(null, initMethodParameter(Service.class, "executeMono", 0)))
				.withMessage("Missing payload");
	}

	@Test
	void nullPayloadWithNullable() {
		boolean resolved = execute(null, initMethodParameter(Service.class, "executeNullable", 0));
		assertNullValues(resolved);

		boolean resolvedMono = execute(null, initMethodParameter(Service.class, "executeNullableMono", 0));
		assertNullValues(resolvedMono);
	}

	@Test
	void nullPayloadWithNotRequired() {
		boolean resolved = execute(null, initMethodParameter(Service.class, "executeNotRequired", 0));
		assertNullValues(resolved);

		boolean resolvedMono = execute(null, initMethodParameter(Service.class, "executeNotRequiredMono", 0));
		assertNullValues(resolvedMono);
	}

	private void assertPayload(boolean resolved, String payload) {
		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getPayloadValue()).isEqualTo(payload);
		assertThat(getRequestValues().getPayload()).isNull();
	}

	private void assertPayloadMono(boolean resolved, Mono<String> payloadMono) {
		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getPayloadValue()).isNull();
		assertThat(getRequestValues().getPayload()).isSameAs(payloadMono);
		assertThat(getRequestValues().getPayloadElementType()).isEqualTo(new ParameterizedTypeReference<String>() { });
	}

	private void assertNullValues(boolean resolved) {
		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getPayloadValue()).isNull();
		assertThat(getRequestValues().getPayload()).isNull();
		assertThat(getRequestValues().getPayloadElementType()).isNull();
	}

	@SuppressWarnings({"unused"})
	private interface Service {

		void execute(@Payload String body);

		void executeNotRequired(@Payload(required = false) String body);

		void executeNullable(@Nullable @Payload String body);

		void executeMono(@Payload Mono<String> body);

		void executeNullableMono(@Nullable @Payload Mono<String> body);

		void executeNotRequiredMono(@Payload(required = false) Mono<String> body);

		void executeSingle(@Payload Single<String> body);

		void executeMonoVoid(@Payload Mono<Void> body);

		void executeCompletable(@Payload Completable body);

		void executeNotAnnotated(String body);

	}

}
