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

package org.springframework.web.service.invoker;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for testing reactive scenarios in {@link HttpServiceMethod} with
 * a test {@link TestHttpClientAdapter} and a test {@link TestHttpExchangeAdapter}
 * that stub the client invocations.
 *
 * <p>
 * The tests do not create or invoke {@code HttpServiceMethod} directly but rather use
 * {@link HttpServiceProxyFactory} to create a service proxy in order to use a strongly
 * typed interface without the need for class casts.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
abstract class ReactiveHttpServiceMethodTests extends HttpServiceMethodTests {

	@Test
	void reactorService() {
		ReactorService service = proxyFactory.createClient(ReactorService.class);

		Mono<Void> voidMono = service.execute();
		StepVerifier.create(voidMono).verifyComplete();
		verifyClientInvocation("void", null);

		Mono<HttpHeaders> headersMono = service.getHeaders();
		StepVerifier.create(headersMono).expectNextCount(1).verifyComplete();
		verifyClientInvocation("headers", null);

		Mono<String> body = service.getBody();
		StepVerifier.create(body).expectNext("body").verifyComplete();
		verifyClientInvocation("body", BODY_TYPE);

		Flux<String> fluxBody = service.getFluxBody();
		StepVerifier.create(fluxBody).expectNext("request", "To", "Body", "Flux").verifyComplete();
		verifyClientInvocation("bodyFlux", BODY_TYPE);

		Mono<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
		StepVerifier.create(voidEntity).expectNext(ResponseEntity.ok().build()).verifyComplete();
		verifyClientInvocation("bodilessEntity", null);

		Mono<ResponseEntity<String>> entity = service.getEntity();
		StepVerifier.create(entity).expectNext(ResponseEntity.ok("requestToEntity"));
		verifyClientInvocation("entity", BODY_TYPE);

		Mono<ResponseEntity<Flux<String>>> fluxEntity = service.getFluxEntity();
		StepVerifier.create(fluxEntity.flatMapMany(HttpEntity::getBody))
				.expectNext("request", "To", "Entity", "Flux")
				.verifyComplete();
		verifyClientInvocation("entityFlux", BODY_TYPE);

		assertThat(service.getDefaultMethodValue()).isEqualTo("default value");
	}

	@Test
	void rxJavaService() {
		RxJavaService service = this.proxyFactory.createClient(RxJavaService.class);
		Completable completable = service.execute();
		assertThat(completable).isNotNull();

		Single<HttpHeaders> headersSingle = service.getHeaders();
		assertThat(headersSingle.blockingGet()).isNotNull();

		Single<String> bodySingle = service.getBody();
		assertThat(bodySingle.blockingGet()).isEqualTo("body");

		Flowable<String> bodyFlow = service.getFlowableBody();
		assertThat(bodyFlow.toList().blockingGet()).asList().containsExactly("request", "To", "Body", "Flux");

		Single<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
		assertThat(voidEntity.blockingGet().getBody()).isNull();

		Single<ResponseEntity<String>> entitySingle = service.getEntity();
		assertThat(entitySingle.blockingGet().getBody()).isEqualTo("entity");

		Single<ResponseEntity<Flowable<String>>> entityFlow = service.getFlowableEntity();
		Flowable<String> body = (entityFlow.blockingGet()).getBody();
		assertThat(body.toList().blockingGet()).containsExactly("request", "To", "Entity", "Flux");
	}

	private interface ReactorService {

		@GetExchange
		Mono<Void> execute();

		@GetExchange
		Mono<HttpHeaders> getHeaders();

		@GetExchange
		Mono<String> getBody();

		@GetExchange
		Flux<String> getFluxBody();

		@GetExchange
		Mono<ResponseEntity<Void>> getVoidEntity();

		@GetExchange
		Mono<ResponseEntity<String>> getEntity();

		@GetExchange
		Mono<ResponseEntity<Flux<String>>> getFluxEntity();

		default String getDefaultMethodValue() {
			return "default value";
		}

	}

	@SuppressWarnings("unused")
	private interface RxJavaService {

		@GetExchange
		Completable execute();

		@GetExchange
		Single<HttpHeaders> getHeaders();

		@GetExchange
		Single<String> getBody();

		@GetExchange
		Flowable<String> getFlowableBody();

		@GetExchange
		Single<ResponseEntity<Void>> getVoidEntity();

		@GetExchange
		Single<ResponseEntity<String>> getEntity();

		@GetExchange
		Single<ResponseEntity<Flowable<String>>> getFlowableEntity();

	}

}
