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

import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
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
 * Base class for testing {@link HttpServiceMethod} with a test {@link TestHttpClientAdapter}
 * and a test {@link TestHttpExchangeAdapter} that stub the client invocations.
 *
 * <p>
 * The tests do not create or invoke {@code HttpServiceMethod} directly but rather use
 * {@link HttpServiceProxyFactory} to create a service proxy in order to use a strongly
 * typed interface without the need for class casts.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
abstract class HttpServiceMethodTests {

	protected static final ParameterizedTypeReference<String> BODY_TYPE = new ParameterizedTypeReference<>() {
	};

	protected TestAdapter client;

	protected HttpServiceProxyFactory proxyFactory;

	@Test
	void blockingService() {
		BlockingService service = this.proxyFactory.createClient(BlockingService.class);

		service.execute();

		HttpHeaders headers = service.getHeaders();
		assertThat(headers).isNotNull();

		String body = service.getBody();
		assertThat(body).isEqualTo(client.getInvokedMethodReference());

		Optional<String> optional = service.getBodyOptional();
		assertThat(optional).contains("body");

		ResponseEntity<String> entity = service.getEntity();
		assertThat(entity.getBody()).isEqualTo("entity");

		ResponseEntity<Void> voidEntity = service.getVoidEntity();
		assertThat(voidEntity.getBody()).isNull();

		List<String> list = service.getList();
		assertThat(list.get(0)).isEqualTo("body");
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
		HttpExchangeAdapter actualClient = this.client instanceof HttpClientAdapter httpClient
				? httpClient.asHttpExchangeAdapter() : (HttpExchangeAdapter) client;
		HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
			.exchangeAdapter(actualClient)
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

	protected void verifyClientInvocation(String methodName, @Nullable ParameterizedTypeReference<?> expectedBodyType) {
		assertThat(this.client.getInvokedMethodReference()).isEqualTo(methodName);
		assertThat(this.client.getBodyType()).isEqualTo(expectedBodyType);
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

		@GetExchange
		List<String> getList();

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
