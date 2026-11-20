/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import java.time.Duration;
import java.util.Collections;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * {@link ReactorHttpExchangeAdapter} with stubbed responses.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("unchecked")
class TestReactorExchangeAdapter extends TestExchangeAdapter implements ReactorHttpExchangeAdapter {

	@Override
	public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
		return ReactiveAdapterRegistry.getSharedInstance();
	}

	@Override
	public Duration getBlockTimeout() {
		return Duration.ofSeconds(5);
	}

	@Override
	public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
		saveInput("exchangeForMono", requestValues, null);
		return Mono.empty();
	}

	@Override
	public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
		saveInput("exchangeForHeadersMono", requestValues, null);
		return Mono.just(new HttpHeaders());
	}

	@Override
	public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		saveInput("exchangeForBodyMono", requestValues, bodyType);
		return bodyType.getType().getTypeName().contains("List") ?
				(Mono<T>) Mono.just(Collections.singletonList(getInvokedMethodName())) :
				(Mono<T>) Mono.just(getInvokedMethodName());
	}

	@Override
	public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		saveInput("exchangeForBodyFlux", requestValues, bodyType);
		return (Flux<T>) Flux.just("exchange", "For", "Body", "Flux");
	}

	@Override
	public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
		saveInput("exchangeForBodilessEntityMono", requestValues, null);
		return Mono.just(ResponseEntity.ok().build());
	}

	@Override
	public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(
			HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

		saveInput("exchangeForEntityMono", requestValues, bodyType);
		return Mono.just((ResponseEntity<T>) ResponseEntity.ok("exchangeForEntityMono"));
	}

	@Override
	public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(
			HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

		saveInput("exchangeForEntityFlux", requestValues, bodyType);
		return Mono.just(ResponseEntity.ok((Flux<T>) Flux.just("exchange", "For", "Entity", "Flux")));
	}

}
