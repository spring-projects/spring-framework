/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.client;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration tests for {@link AsyncRestTemplate}.
 *
 * <h3>Logging configuration for {@code MockWebServer}</h3>
 *
 * <p>In order for our log4j2 configuration to be used in an IDE, you must
 * set the following system property before running any tests &mdash; for
 * example, in <em>Run Configurations</em> in Eclipse.
 *
 * <pre class="code">
 * -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
@SuppressWarnings("deprecation")
public class AsyncRestTemplateIntegrationTests extends AbstractMockWebServerTests {

	private final AsyncRestTemplate template = new AsyncRestTemplate(
			new org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory());


	@Test
	public void getEntity() throws Exception {
		Future<ResponseEntity<String>> future = template.getForEntity(baseUrl + "/{method}", String.class, "get");
		ResponseEntity<String> entity = future.get();
		assertThat(entity.getBody()).as("Invalid content").isEqualTo(helloWorld);
		assertThat(entity.getHeaders().isEmpty()).as("No headers").isFalse();
		assertThat(entity.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(textContentType);
		assertThat(entity.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
	}

	@Test
	public void getEntityFromCompletable() throws Exception {
		ListenableFuture<ResponseEntity<String>> future = template.getForEntity(baseUrl + "/{method}", String.class, "get");
		ResponseEntity<String> entity = future.completable().get();
		assertThat(entity.getBody()).as("Invalid content").isEqualTo(helloWorld);
		assertThat(entity.getHeaders().isEmpty()).as("No headers").isFalse();
		assertThat(entity.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(textContentType);
		assertThat(entity.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
	}

	@Test
	public void multipleFutureGets() throws Exception {
		Future<ResponseEntity<String>> future = template.getForEntity(baseUrl + "/{method}", String.class, "get");
		future.get();
		future.get();
	}

	@Test
	public void getEntityCallback() throws Exception {
		ListenableFuture<ResponseEntity<String>> futureEntity =
				template.getForEntity(baseUrl + "/{method}", String.class, "get");
		futureEntity.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
			@Override
			public void onSuccess(ResponseEntity<String> entity) {
				assertThat(entity.getBody()).as("Invalid content").isEqualTo(helloWorld);
				assertThat(entity.getHeaders().isEmpty()).as("No headers").isFalse();
				assertThat(entity.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(textContentType);
				assertThat(entity.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		waitTillDone(futureEntity);
	}

	@Test
	public void getEntityCallbackWithLambdas() throws Exception {
		ListenableFuture<ResponseEntity<String>> futureEntity =
				template.getForEntity(baseUrl + "/{method}", String.class, "get");
		futureEntity.addCallback(entity -> {
			assertThat(entity.getBody()).as("Invalid content").isEqualTo(helloWorld);
			assertThat(entity.getHeaders().isEmpty()).as("No headers").isFalse();
			assertThat(entity.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(textContentType);
			assertThat(entity.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
		}, ex -> fail(ex.getMessage()));
		waitTillDone(futureEntity);
	}

	@Test
	public void getNoResponse() throws Exception {
		Future<ResponseEntity<String>> futureEntity = template.getForEntity(baseUrl + "/get/nothing", String.class);
		ResponseEntity<String> entity = futureEntity.get();
		assertThat(entity.getBody()).as("Invalid content").isNull();
	}

	@Test
	public void getNoContentTypeHeader() throws Exception {
		Future<ResponseEntity<byte[]>> futureEntity = template.getForEntity(baseUrl + "/get/nocontenttype", byte[].class);
		ResponseEntity<byte[]> responseEntity = futureEntity.get();
		assertThat(responseEntity.getBody()).as("Invalid content").isEqualTo(helloWorld.getBytes("UTF-8"));
	}

	@Test
	public void getNoContent() throws Exception {
		Future<ResponseEntity<String>> responseFuture = template.getForEntity(baseUrl + "/status/nocontent", String.class);
		ResponseEntity<String> entity = responseFuture.get();
		assertThat(entity.getStatusCode()).as("Invalid response code").isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(entity.getBody()).as("Invalid content").isNull();
	}

	@Test
	public void getNotModified() throws Exception {
		Future<ResponseEntity<String>> responseFuture = template.getForEntity(baseUrl + "/status/notmodified", String.class);
		ResponseEntity<String> entity = responseFuture.get();
		assertThat(entity.getStatusCode()).as("Invalid response code").isEqualTo(HttpStatus.NOT_MODIFIED);
		assertThat(entity.getBody()).as("Invalid content").isNull();
	}

	@Test
	public void headForHeaders() throws Exception {
		Future<HttpHeaders> headersFuture = template.headForHeaders(baseUrl + "/get");
		HttpHeaders headers = headersFuture.get();
		assertThat(headers.containsKey("Content-Type")).as("No Content-Type header").isTrue();
	}

	@Test
	public void headForHeadersCallback() throws Exception {
		ListenableFuture<HttpHeaders> headersFuture = template.headForHeaders(baseUrl + "/get");
		headersFuture.addCallback(new ListenableFutureCallback<HttpHeaders>() {
			@Override
			public void onSuccess(HttpHeaders result) {
				assertThat(result.containsKey("Content-Type")).as("No Content-Type header").isTrue();
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		waitTillDone(headersFuture);
	}

	@Test
	public void headForHeadersCallbackWithLambdas() throws Exception {
		ListenableFuture<HttpHeaders> headersFuture = template.headForHeaders(baseUrl + "/get");
		headersFuture.addCallback(result -> assertThat(result.containsKey("Content-Type")).as("No Content-Type header").isTrue(), ex -> fail(ex.getMessage()));
		waitTillDone(headersFuture);
	}

	@Test
	public void postForLocation() throws Exception  {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
		Future<URI> locationFuture = template.postForLocation(baseUrl + "/{method}", entity, "post");
		URI location = locationFuture.get();
		assertThat(location).as("Invalid location").isEqualTo(new URI(baseUrl + "/post/1"));
	}

	@Test
	public void postForLocationCallback() throws Exception  {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
		final URI expected = new URI(baseUrl + "/post/1");
		ListenableFuture<URI> locationFuture = template.postForLocation(baseUrl + "/{method}", entity, "post");
		locationFuture.addCallback(new ListenableFutureCallback<URI>() {
			@Override
			public void onSuccess(URI result) {
				assertThat(result).as("Invalid location").isEqualTo(expected);
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		waitTillDone(locationFuture);
	}

	@Test
	public void postForLocationCallbackWithLambdas() throws Exception  {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
		final URI expected = new URI(baseUrl + "/post/1");
		ListenableFuture<URI> locationFuture = template.postForLocation(baseUrl + "/{method}", entity, "post");
		locationFuture.addCallback(result -> assertThat(result).as("Invalid location").isEqualTo(expected),
				ex -> fail(ex.getMessage()));
		waitTillDone(locationFuture);
	}

	@Test
	public void postForEntity() throws Exception  {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		Future<ResponseEntity<String>> responseEntityFuture =
				template.postForEntity(baseUrl + "/{method}", requestEntity, String.class, "post");
		ResponseEntity<String> responseEntity = responseEntityFuture.get();
		assertThat(responseEntity.getBody()).as("Invalid content").isEqualTo(helloWorld);
	}

	@Test
	public void postForEntityCallback() throws Exception  {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		ListenableFuture<ResponseEntity<String>> responseEntityFuture =
				template.postForEntity(baseUrl + "/{method}", requestEntity, String.class, "post");
		responseEntityFuture.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
			@Override
			public void onSuccess(ResponseEntity<String> result) {
				assertThat(result.getBody()).as("Invalid content").isEqualTo(helloWorld);
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		waitTillDone(responseEntityFuture);
	}

	@Test
	public void postForEntityCallbackWithLambdas() throws Exception  {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		ListenableFuture<ResponseEntity<String>> responseEntityFuture =
				template.postForEntity(baseUrl + "/{method}", requestEntity, String.class, "post");
		responseEntityFuture.addCallback(
				result -> assertThat(result.getBody()).as("Invalid content").isEqualTo(helloWorld),
				ex -> fail(ex.getMessage()));
		waitTillDone(responseEntityFuture);
	}

	@Test
	public void put() throws Exception  {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		Future<?> responseEntityFuture = template.put(baseUrl + "/{method}", requestEntity, "put");
		responseEntityFuture.get();
	}

	@Test
	public void putCallback() throws Exception  {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		ListenableFuture<?> responseEntityFuture = template.put(baseUrl + "/{method}", requestEntity, "put");
		responseEntityFuture.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onSuccess(Object result) {
				assertThat(result).isNull();
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		waitTillDone(responseEntityFuture);
	}

	@Test
	public void delete() throws Exception  {
		Future<?> deletedFuture = template.delete(new URI(baseUrl + "/delete"));
		deletedFuture.get();
	}

	@Test
	public void deleteCallback() throws Exception  {
		ListenableFuture<?> deletedFuture = template.delete(new URI(baseUrl + "/delete"));
		deletedFuture.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onSuccess(Object result) {
				assertThat(result).isNull();
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		waitTillDone(deletedFuture);
	}

	@Test
	public void deleteCallbackWithLambdas() throws Exception  {
		ListenableFuture<?> deletedFuture = template.delete(new URI(baseUrl + "/delete"));
		deletedFuture.addCallback(result -> assertThat(result).isNull(), ex -> fail(ex.getMessage()));
		waitTillDone(deletedFuture);
	}

	@Test
	public void identicalExceptionThroughGetAndCallback() throws Exception {
		final HttpClientErrorException[] callbackException = new HttpClientErrorException[1];

		final CountDownLatch latch = new CountDownLatch(1);
		ListenableFuture<?> future = template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null);
		future.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onSuccess(Object result) {
				fail("onSuccess not expected");
			}
			@Override
			public void onFailure(Throwable ex) {
				boolean condition = ex instanceof HttpClientErrorException;
				assertThat(condition).isTrue();
				callbackException[0] = (HttpClientErrorException) ex;
				latch.countDown();
			}
		});

		try {
			future.get();
			fail("Exception expected");
		}
		catch (ExecutionException ex) {
			Throwable cause = ex.getCause();
			boolean condition = cause instanceof HttpClientErrorException;
			assertThat(condition).isTrue();
			latch.await(5, TimeUnit.SECONDS);
			assertThat(cause).isSameAs(callbackException[0]);
		}
	}

	@Test
	public void notFoundGet() throws Exception {
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> {
				Future<?> future = template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null);
				future.get();
				})
			.withCauseInstanceOf(HttpClientErrorException.class)
			.satisfies(ex -> {
				HttpClientErrorException cause = (HttpClientErrorException) ex.getCause();
				assertThat(cause.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(cause.getStatusText()).isNotNull();
				assertThat(cause.getResponseBodyAsString()).isNotNull();
			});
	}

	@Test
	public void notFoundCallback() throws Exception {
		ListenableFuture<?> future = template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null);
		future.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onSuccess(Object result) {
				fail("onSuccess not expected");
			}
			@Override
			public void onFailure(Throwable t) {
				boolean condition = t instanceof HttpClientErrorException;
				assertThat(condition).isTrue();
				HttpClientErrorException ex = (HttpClientErrorException) t;
				assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(ex.getStatusText()).isNotNull();
				assertThat(ex.getResponseBodyAsString()).isNotNull();
			}
		});
		waitTillDone(future);
	}

	@Test
	public void notFoundCallbackWithLambdas() throws Exception {
		ListenableFuture<?> future = template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null);
		future.addCallback(result -> fail("onSuccess not expected"), ex -> {
			boolean condition = ex instanceof HttpClientErrorException;
			assertThat(condition).isTrue();
			HttpClientErrorException hcex = (HttpClientErrorException) ex;
			assertThat(hcex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(hcex.getStatusText()).isNotNull();
			assertThat(hcex.getResponseBodyAsString()).isNotNull();
		});
		waitTillDone(future);
	}

	@Test
	public void serverError() throws Exception {
		try {
			Future<Void> future = template.execute(baseUrl + "/status/server", HttpMethod.GET, null, null);
			future.get();
			fail("HttpServerErrorException expected");
		}
		catch (ExecutionException ex) {
			boolean condition = ex.getCause() instanceof HttpServerErrorException;
			assertThat(condition).isTrue();
			HttpServerErrorException cause = (HttpServerErrorException)ex.getCause();

			assertThat(cause.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			assertThat(cause.getStatusText()).isNotNull();
			assertThat(cause.getResponseBodyAsString()).isNotNull();
		}
	}

	@Test
	public void serverErrorCallback() throws Exception {
		ListenableFuture<Void> future = template.execute(baseUrl + "/status/server", HttpMethod.GET, null, null);
		future.addCallback(new ListenableFutureCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				fail("onSuccess not expected");
			}
			@Override
			public void onFailure(Throwable ex) {
				boolean condition = ex instanceof HttpServerErrorException;
				assertThat(condition).isTrue();
				HttpServerErrorException hsex = (HttpServerErrorException) ex;
				assertThat(hsex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
				assertThat(hsex.getStatusText()).isNotNull();
				assertThat(hsex.getResponseBodyAsString()).isNotNull();
			}
		});
		waitTillDone(future);
	}

	@Test
	public void serverErrorCallbackWithLambdas() throws Exception {
		ListenableFuture<Void> future = template.execute(baseUrl + "/status/server", HttpMethod.GET, null, null);
		future.addCallback(result -> fail("onSuccess not expected"), ex -> {
			boolean condition = ex instanceof HttpServerErrorException;
			assertThat(condition).isTrue();
			HttpServerErrorException hsex = (HttpServerErrorException) ex;
			assertThat(hsex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			assertThat(hsex.getStatusText()).isNotNull();
			assertThat(hsex.getResponseBodyAsString()).isNotNull();
		});
		waitTillDone(future);
	}

	@Test
	public void optionsForAllow() throws Exception {
		Future<Set<HttpMethod>> allowedFuture = template.optionsForAllow(new URI(baseUrl + "/get"));
		Set<HttpMethod> allowed = allowedFuture.get();
		assertThat(allowed).as("Invalid response").isEqualTo(EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE));
	}

	@Test
	public void optionsForAllowCallback() throws Exception {
		ListenableFuture<Set<HttpMethod>> allowedFuture = template.optionsForAllow(new URI(baseUrl + "/get"));
		allowedFuture.addCallback(new ListenableFutureCallback<Set<HttpMethod>>() {
			@Override
			public void onSuccess(Set<HttpMethod> result) {
				assertThat(result).as("Invalid response").isEqualTo(EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS,
								HttpMethod.HEAD, HttpMethod.TRACE));
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		waitTillDone(allowedFuture);
	}

	@Test
	public void optionsForAllowCallbackWithLambdas() throws Exception{
		ListenableFuture<Set<HttpMethod>> allowedFuture = template.optionsForAllow(new URI(baseUrl + "/get"));
		allowedFuture.addCallback(result -> assertThat(result).as("Invalid response").isEqualTo(EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD,HttpMethod.TRACE)),
				ex -> fail(ex.getMessage()));
		waitTillDone(allowedFuture);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void exchangeGet() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		HttpEntity<?> requestEntity = new HttpEntity(requestHeaders);
		Future<ResponseEntity<String>> responseFuture =
				template.exchange(baseUrl + "/{method}", HttpMethod.GET, requestEntity, String.class, "get");
		ResponseEntity<String> response = responseFuture.get();
		assertThat(response.getBody()).as("Invalid content").isEqualTo(helloWorld);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void exchangeGetCallback() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		HttpEntity<?> requestEntity = new HttpEntity(requestHeaders);
		ListenableFuture<ResponseEntity<String>> responseFuture =
				template.exchange(baseUrl + "/{method}", HttpMethod.GET, requestEntity, String.class, "get");
		responseFuture.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
			@Override
			public void onSuccess(ResponseEntity<String> result) {
				assertThat(result.getBody()).as("Invalid content").isEqualTo(helloWorld);
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		waitTillDone(responseFuture);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void exchangeGetCallbackWithLambdas() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		HttpEntity<?> requestEntity = new HttpEntity(requestHeaders);
		ListenableFuture<ResponseEntity<String>> responseFuture =
				template.exchange(baseUrl + "/{method}", HttpMethod.GET, requestEntity, String.class, "get");
		responseFuture.addCallback(result -> assertThat(result.getBody()).as("Invalid content").isEqualTo(helloWorld), ex -> fail(ex.getMessage()));
		waitTillDone(responseFuture);
	}

	@Test
	public void exchangePost() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		requestHeaders.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld, requestHeaders);
		Future<ResponseEntity<Void>> resultFuture =
				template.exchange(baseUrl + "/{method}", HttpMethod.POST, requestEntity, Void.class, "post");
		ResponseEntity<Void> result = resultFuture.get();
		assertThat(result.getHeaders().getLocation()).as("Invalid location").isEqualTo(new URI(baseUrl + "/post/1"));
		assertThat(result.hasBody()).isFalse();
	}

	@Test
	public void exchangePostCallback() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		requestHeaders.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld, requestHeaders);
		ListenableFuture<ResponseEntity<Void>> resultFuture =
				template.exchange(baseUrl + "/{method}", HttpMethod.POST, requestEntity, Void.class, "post");
		final URI expected =new URI(baseUrl + "/post/1");
		resultFuture.addCallback(new ListenableFutureCallback<ResponseEntity<Void>>() {
			@Override
			public void onSuccess(ResponseEntity<Void> result) {
				assertThat(result.getHeaders().getLocation()).as("Invalid location").isEqualTo(expected);
				assertThat(result.hasBody()).isFalse();
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		waitTillDone(resultFuture);
	}

	@Test
	public void exchangePostCallbackWithLambdas() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		requestHeaders.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld, requestHeaders);
		ListenableFuture<ResponseEntity<Void>> resultFuture =
				template.exchange(baseUrl + "/{method}", HttpMethod.POST, requestEntity, Void.class, "post");
		final URI expected =new URI(baseUrl + "/post/1");
		resultFuture.addCallback(result -> {
			assertThat(result.getHeaders().getLocation()).as("Invalid location").isEqualTo(expected);
			assertThat(result.hasBody()).isFalse();
		}, ex -> fail(ex.getMessage()));
		waitTillDone(resultFuture);
	}

	@Test
	public void multipartFormData() throws Exception {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("name 1", "value 1");
		parts.add("name 2", "value 2+1");
		parts.add("name 2", "value 2+2");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);

		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(parts);
		Future<URI> future = template.postForLocation(baseUrl + "/multipartFormData", requestBody);
		future.get();
	}

	@Test
	public void getAndInterceptResponse() throws Exception {
		RequestInterceptor interceptor = new RequestInterceptor();
		template.setInterceptors(Collections.singletonList(interceptor));
		ListenableFuture<ResponseEntity<String>> future = template.getForEntity(baseUrl + "/get", String.class);

		interceptor.latch.await(5, TimeUnit.SECONDS);
		assertThat(interceptor.response).isNotNull();
		assertThat(interceptor.response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(interceptor.exception).isNull();
		assertThat(future.get().getBody()).isEqualTo(helloWorld);
	}

	@Test
	public void getAndInterceptError() throws Exception {
		RequestInterceptor interceptor = new RequestInterceptor();
		template.setInterceptors(Collections.singletonList(interceptor));
		template.getForEntity(baseUrl + "/status/notfound", String.class);

		interceptor.latch.await(5, TimeUnit.SECONDS);
		assertThat(interceptor.response).isNotNull();
		assertThat(interceptor.response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(interceptor.exception).isNull();
	}

	private void waitTillDone(ListenableFuture<?> future) {
		while (!future.isDone()) {
			try {
				Thread.sleep(5);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}


	private static class RequestInterceptor implements org.springframework.http.client.AsyncClientHttpRequestInterceptor {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile ClientHttpResponse response;

		private volatile Throwable exception;

		@Override
		public ListenableFuture<ClientHttpResponse> intercept(HttpRequest request, byte[] body,
				org.springframework.http.client.AsyncClientHttpRequestExecution execution) throws IOException {

			ListenableFuture<ClientHttpResponse> future = execution.executeAsync(request, body);
			future.addCallback(
					resp -> {
						response = resp;
						this.latch.countDown();
					},
					ex -> {
						exception = ex;
						this.latch.countDown();
					});
			return future;
		}
	}

}
