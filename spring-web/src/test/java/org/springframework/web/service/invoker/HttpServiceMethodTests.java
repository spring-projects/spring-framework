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

package org.springframework.web.service.invoker;

import java.util.Optional;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
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
 * Tests for {@link HttpServiceMethod} with a test {@link TestHttpClientAdapter}
 * that stubs the client invocations.
 *
 * <p>The tests do not create or invoke {@code HttpServiceMethod} directly but
 * rather use {@link HttpServiceProxyFactory} to create a service proxy in order to
 * use a strongly typed interface without the need for class casts.
 *
 * @author Rossen Stoyanchev
 */
public class HttpServiceMethodTests {

	private static final ParameterizedTypeReference<String> BODY_TYPE = new ParameterizedTypeReference<>() {};


	private final TestHttpClientAdapter client = new TestHttpClientAdapter();

	private HttpServiceProxyFactory proxyFactory;


	@BeforeEach
	void setUp() throws Exception {
		this.proxyFactory = new HttpServiceProxyFactory(this.client);
		this.proxyFactory.afterPropertiesSet();
	}


	@Test
	void reactorService() {
		ReactorService service = this.proxyFactory.createClient(ReactorService.class);

		Mono<Void> voidMono = service.execute();
		StepVerifier.create(voidMono).verifyComplete();
		verifyClientInvocation("requestToVoid", null);

		Mono<HttpHeaders> headersMono = service.getHeaders();
		StepVerifier.create(headersMono).expectNextCount(1).verifyComplete();
		verifyClientInvocation("requestToHeaders", null);

		Mono<String> body = service.getBody();
		StepVerifier.create(body).expectNext("requestToBody").verifyComplete();
		verifyClientInvocation("requestToBody", BODY_TYPE);

		Flux<String> fluxBody = service.getFluxBody();
		StepVerifier.create(fluxBody).expectNext("request", "To", "Body", "Flux").verifyComplete();
		verifyClientInvocation("requestToBodyFlux", BODY_TYPE);

		Mono<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
		StepVerifier.create(voidEntity).expectNext(ResponseEntity.ok().build()).verifyComplete();
		verifyClientInvocation("requestToBodilessEntity", null);

		Mono<ResponseEntity<String>> entity = service.getEntity();
		StepVerifier.create(entity).expectNext(ResponseEntity.ok("requestToEntity"));
		verifyClientInvocation("requestToEntity", BODY_TYPE);

		Mono<ResponseEntity<Flux<String>>> fluxEntity= service.getFluxEntity();
		StepVerifier.create(fluxEntity.flatMapMany(HttpEntity::getBody)).expectNext("request", "To", "Entity", "Flux").verifyComplete();
		verifyClientInvocation("requestToEntityFlux", BODY_TYPE);

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
		assertThat(bodySingle.blockingGet()).isEqualTo("requestToBody");

		Flowable<String> bodyFlow = service.getFlowableBody();
		assertThat(bodyFlow.toList().blockingGet()).asList().containsExactly("request", "To", "Body", "Flux");

		Single<ResponseEntity<Void>> voidEntity = service.getVoidEntity();
		assertThat(voidEntity.blockingGet().getBody()).isNull();

		Single<ResponseEntity<String>> entitySingle = service.getEntity();
		assertThat(entitySingle.blockingGet().getBody()).isEqualTo("requestToEntity");

		Single<ResponseEntity<Flowable<String>>> entityFlow = service.getFlowableEntity();
		Flowable<String> body = (entityFlow.blockingGet()).getBody();
		assertThat(body.toList().blockingGet()).containsExactly("request", "To", "Entity", "Flux");
	}

	@Test
	void blockingService() {
		BlockingService service = this.proxyFactory.createClient(BlockingService.class);

		service.execute();

		HttpHeaders headers = service.getHeaders();
		assertThat(headers).isNotNull();

		String body = service.getBody();
		assertThat(body).isEqualTo("requestToBody");

		Optional<String> optional = service.getBodyOptional();
		assertThat(optional).contains("requestToBody");

		ResponseEntity<String> entity = service.getEntity();
		assertThat(entity.getBody()).isEqualTo("requestToEntity");

		ResponseEntity<Void> voidEntity = service.getVoidEntity();
		assertThat(voidEntity.getBody()).isNull();
	}

	@Test
	void methodAnnotatedService() {
		MethodLevelAnnotatedService service = this.proxyFactory.createClient(MethodLevelAnnotatedService.class);

		service.performGet();

		HttpRequestValues requestValues = this.client.getRequestValues();
		assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.GET);
		assertThat(requestValues.getUriTemplate()).isEqualTo("");
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
	void typeAndMethodAnnotatedService() throws Exception {
		HttpServiceProxyFactory proxyFactory = new HttpServiceProxyFactory(this.client);
		proxyFactory.setEmbeddedValueResolver(value -> (value.equals("${baseUrl}") ? "/base" : value));
		proxyFactory.afterPropertiesSet();

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

	private void verifyClientInvocation(String methodName, @Nullable ParameterizedTypeReference<?> expectedBodyType) {
		assertThat(this.client.getInvokedMethodName()).isEqualTo(methodName);
		assertThat(this.client.getBodyType()).isEqualTo(expectedBodyType);
	}


	@SuppressWarnings("unused")
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
	private interface BlockingService {

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
