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

import java.util.List;
import java.util.Optional;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_CBOR_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Tests for {@link HttpServiceMethod} with
 * {@link TestExchangeAdapter} and {@link TestReactorExchangeAdapter}.
 *
 * <p>The tests do not create or invoke {@code HttpServiceMethod} directly but
 * rather use {@link HttpServiceProxyFactory} to create a service proxy in order to
 * use a strongly typed interface without the need for class casts.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
class HttpServiceMethodTests {

	private static final ParameterizedTypeReference<String> BODY_TYPE = new ParameterizedTypeReference<>() {};


	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final TestReactorExchangeAdapter reactorClient = new TestReactorExchangeAdapter();

	private final HttpServiceProxyFactory proxyFactory =
			HttpServiceProxyFactory.builder().exchangeAdapter(this.client).build();

	private final HttpServiceProxyFactory reactorProxyFactory =
			HttpServiceProxyFactory.builder().exchangeAdapter(this.reactorClient).build();


	@Test
	void service() {
		Service service = this.proxyFactory.createClient(Service.class);

		service.execute();

		HttpHeaders headers = service.getHeaders();
		assertThat(headers).isNotNull();

		String body = service.getBody();
		assertThat(body).isEqualTo(this.client.getInvokedMethodName());

		Optional<String> optional = service.getBodyOptional();
		assertThat(optional.get()).isEqualTo("exchangeForBody");

		ResponseEntity<String> entity = service.getEntity();
		assertThat(entity.getBody()).isEqualTo("exchangeForEntity");

		ResponseEntity<Void> voidEntity = service.getVoidEntity();
		assertThat(voidEntity.getBody()).isNull();

		List<String> list = service.getList();
		assertThat(list.get(0)).isEqualTo("exchangeForBody");
	}

	@Test
	void reactorService() {
		ReactorService service = this.reactorProxyFactory.createClient(ReactorService.class);

		Mono<Void> voidMono = service.execute();
		StepVerifier.create(voidMono).verifyComplete();
		verifyReactorClientInvocation("exchangeForMono", null);

		Mono<HttpHeaders> headersMono = service.getHeaders();
		StepVerifier.create(headersMono).expectNextCount(1).verifyComplete();
		verifyReactorClientInvocation("exchangeForHeadersMono", null);

		Mono<String> body = service.getBody();
		StepVerifier.create(body).expectNext("exchangeForBodyMono").verifyComplete();
		verifyReactorClientInvocation("exchangeForBodyMono", BODY_TYPE);

		Flux<String> fluxBody = service.getFluxBody();
		StepVerifier.create(fluxBody).expectNext("exchange", "For", "Body", "Flux").verifyComplete();
		verifyReactorClientInvocation("exchangeForBodyFlux", BODY_TYPE);

		Mono<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
		StepVerifier.create(voidEntity).expectNext(ResponseEntity.ok().build()).verifyComplete();
		verifyReactorClientInvocation("exchangeForBodilessEntityMono", null);

		Mono<ResponseEntity<String>> entity = service.getEntity();
		StepVerifier.create(entity).expectNext(ResponseEntity.ok("exchangeForEntityMono"));
		verifyReactorClientInvocation("exchangeForEntityMono", BODY_TYPE);

		Mono<ResponseEntity<Flux<String>>> fluxEntity = service.getFluxEntity();
		StepVerifier.create(fluxEntity.flatMapMany(HttpEntity::getBody))
				.expectNext("exchange", "For", "Entity", "Flux")
				.verifyComplete();
		verifyReactorClientInvocation("exchangeForEntityFlux", BODY_TYPE);

		assertThat(service.getDefaultMethodValue()).isEqualTo("default value");
	}

	@Test
	void rxJavaService() {
		RxJavaService service = this.reactorProxyFactory.createClient(RxJavaService.class);
		Completable completable = service.execute();
		assertThat(completable).isNotNull();

		Single<HttpHeaders> headersSingle = service.getHeaders();
		assertThat(headersSingle.blockingGet()).isNotNull();

		Single<String> bodySingle = service.getBody();
		assertThat(bodySingle.blockingGet()).isEqualTo("exchangeForBodyMono");

		Flowable<String> bodyFlow = service.getFlowableBody();
		assertThat(bodyFlow.toList().blockingGet()).asList().containsExactly("exchange", "For", "Body", "Flux");

		Single<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
		assertThat(voidEntity.blockingGet().getBody()).isNull();

		Single<ResponseEntity<String>> entitySingle = service.getEntity();
		assertThat(entitySingle.blockingGet().getBody()).isEqualTo("exchangeForEntityMono");

		Single<ResponseEntity<Flowable<String>>> entityFlow = service.getFlowableEntity();
		Flowable<String> body = (entityFlow.blockingGet()).getBody();
		assertThat(body.toList().blockingGet()).containsExactly("exchange", "For", "Entity", "Flux");
	}

	@Test
	void methodAnnotatedService() {
		MethodLevelAnnotatedService service = this.proxyFactory.createClient(MethodLevelAnnotatedService.class);

		service.performGet();

		HttpRequestValues requestValues = this.client.getRequestValues();
		assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.GET);
		assertThat(requestValues.getUriTemplate()).isEmpty();
		assertThat(requestValues.getHeaders().getContentType()).isNull();
		assertThat(requestValues.getHeaders().getAccept()).isEmpty();

		service.performPost();

		requestValues = this.client.getRequestValues();
		assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.POST);
		assertThat(requestValues.getUriTemplate()).isEqualTo("/url");
		assertThat(requestValues.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(requestValues.getHeaders().getAccept()).containsExactly(MediaType.APPLICATION_JSON);
	}

	@Test
	void typeAndMethodAnnotatedService() {
		HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
			.exchangeAdapter(this.client)
			.embeddedValueResolver(value -> (value.equals("${baseUrl}") ? "/base" : value))
			.build();

		MethodLevelAnnotatedService service = proxyFactory.createClient(TypeAndMethodLevelAnnotatedService.class);

		service.performGet();

		HttpRequestValues requestValues = this.client.getRequestValues();
		assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.GET);
		assertThat(requestValues.getUriTemplate()).isEqualTo("/base");
		assertThat(requestValues.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_CBOR);
		assertThat(requestValues.getHeaders().getAccept()).containsExactly(MediaType.APPLICATION_CBOR);

		service.performPost();

		requestValues = this.client.getRequestValues();
		assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.POST);
		assertThat(requestValues.getUriTemplate()).isEqualTo("/base/url");
		assertThat(requestValues.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(requestValues.getHeaders().getAccept()).containsExactly(MediaType.APPLICATION_JSON);
	}

	protected void verifyReactorClientInvocation(String methodName, @Nullable ParameterizedTypeReference<?> expectedBodyType) {
		assertThat(this.reactorClient.getInvokedMethodName()).isEqualTo(methodName);
		assertThat(this.reactorClient.getBodyType()).isEqualTo(expectedBodyType);
	}


	@SuppressWarnings("unused")
	private interface Service {

		@GetExchange
		void execute();

		@GetExchange
		HttpHeaders getHeaders();

		@GetExchange
		String getBody();

		@GetExchange
		Optional<String> getBodyOptional();

		@GetExchange
		ResponseEntity<Void> getVoidEntity();

		@GetExchange
		ResponseEntity<String> getEntity();

		@GetExchange
		List<String> getList();

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


	@SuppressWarnings("unused")
	private interface MethodLevelAnnotatedService {

		@GetExchange
		void performGet();

		@PostExchange(url = "/url", contentType = APPLICATION_JSON_VALUE, accept = APPLICATION_JSON_VALUE)
		void performPost();

	}

	@SuppressWarnings("unused")
	@HttpExchange(url = "${baseUrl}", contentType = APPLICATION_CBOR_VALUE, accept = APPLICATION_CBOR_VALUE)
	private interface TypeAndMethodLevelAnnotatedService extends MethodLevelAnnotatedService {
	}

}
