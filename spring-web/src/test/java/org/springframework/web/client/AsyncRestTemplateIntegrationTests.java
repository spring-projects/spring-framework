/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
public class AsyncRestTemplateIntegrationTests extends AbstractMockWebServerTestCase {

	private final AsyncRestTemplate template = new AsyncRestTemplate(
			new HttpComponentsAsyncClientHttpRequestFactory());


	@Test
	public void getEntity() throws Exception {
		Future<ResponseEntity<String>> future = template.getForEntity(baseUrl + "/{method}", String.class, "get");
		ResponseEntity<String> entity = future.get();
		assertEquals("Invalid content", helloWorld, entity.getBody());
		assertFalse("No headers", entity.getHeaders().isEmpty());
		assertEquals("Invalid content-type", textContentType, entity.getHeaders().getContentType());
		assertEquals("Invalid status code", HttpStatus.OK, entity.getStatusCode());
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
				assertEquals("Invalid content", helloWorld, entity.getBody());
				assertFalse("No headers", entity.getHeaders().isEmpty());
				assertEquals("Invalid content-type", textContentType, entity.getHeaders().getContentType());
				assertEquals("Invalid status code", HttpStatus.OK, entity.getStatusCode());
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
		futureEntity.addCallback((entity) -> {
			assertEquals("Invalid content", helloWorld, entity.getBody());
			assertFalse("No headers", entity.getHeaders().isEmpty());
			assertEquals("Invalid content-type", textContentType, entity.getHeaders().getContentType());
			assertEquals("Invalid status code", HttpStatus.OK, entity.getStatusCode());
		}, ex -> fail(ex.getMessage()));
		waitTillDone(futureEntity);
	}

	@Test
	public void getNoResponse() throws Exception {
		Future<ResponseEntity<String>> futureEntity = template.getForEntity(baseUrl + "/get/nothing", String.class);
		ResponseEntity<String> entity = futureEntity.get();
		assertNull("Invalid content", entity.getBody());
	}

	@Test
	public void getNoContentTypeHeader() throws Exception {
		Future<ResponseEntity<byte[]>> futureEntity = template.getForEntity(baseUrl + "/get/nocontenttype", byte[].class);
		ResponseEntity<byte[]> responseEntity = futureEntity.get();
		assertArrayEquals("Invalid content", helloWorld.getBytes("UTF-8"), responseEntity.getBody());
	}

	@Test
	public void getNoContent() throws Exception {
		Future<ResponseEntity<String>> responseFuture = template.getForEntity(baseUrl + "/status/nocontent", String.class);
		ResponseEntity<String> entity = responseFuture.get();
		assertEquals("Invalid response code", HttpStatus.NO_CONTENT, entity.getStatusCode());
		assertNull("Invalid content", entity.getBody());
	}

	@Test
	public void getNotModified() throws Exception {
		Future<ResponseEntity<String>> responseFuture = template.getForEntity(baseUrl + "/status/notmodified", String.class);
		ResponseEntity<String> entity = responseFuture.get();
		assertEquals("Invalid response code", HttpStatus.NOT_MODIFIED, entity.getStatusCode());
		assertNull("Invalid content", entity.getBody());
	}

	@Test
	public void headForHeaders() throws Exception {
		Future<HttpHeaders> headersFuture = template.headForHeaders(baseUrl + "/get");
		HttpHeaders headers = headersFuture.get();
		assertTrue("No Content-Type header", headers.containsKey("Content-Type"));
	}

	@Test
	public void headForHeadersCallback() throws Exception {
		ListenableFuture<HttpHeaders> headersFuture = template.headForHeaders(baseUrl + "/get");
		headersFuture.addCallback(new ListenableFutureCallback<HttpHeaders>() {
			@Override
			public void onSuccess(HttpHeaders result) {
				assertTrue("No Content-Type header", result.containsKey("Content-Type"));
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
		headersFuture.addCallback(result -> assertTrue("No Content-Type header",
				result.containsKey("Content-Type")), ex -> fail(ex.getMessage()));
		waitTillDone(headersFuture);
	}

	@Test
	public void postForLocation() throws Exception  {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", Charset.forName("ISO-8859-1")));
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
		Future<URI> locationFuture = template.postForLocation(baseUrl + "/{method}", entity, "post");
		URI location = locationFuture.get();
		assertEquals("Invalid location", new URI(baseUrl + "/post/1"), location);
	}

	@Test
	public void postForLocationCallback() throws Exception  {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", Charset.forName("ISO-8859-1")));
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
		final URI expected = new URI(baseUrl + "/post/1");
		ListenableFuture<URI> locationFuture = template.postForLocation(baseUrl + "/{method}", entity, "post");
		locationFuture.addCallback(new ListenableFutureCallback<URI>() {
			@Override
			public void onSuccess(URI result) {
				assertEquals("Invalid location", expected, result);
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
		entityHeaders.setContentType(new MediaType("text", "plain", Charset.forName("ISO-8859-1")));
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
		final URI expected = new URI(baseUrl + "/post/1");
		ListenableFuture<URI> locationFuture = template.postForLocation(baseUrl + "/{method}", entity, "post");
		locationFuture.addCallback(result -> assertEquals("Invalid location", expected, result),
				ex -> fail(ex.getMessage()));
		waitTillDone(locationFuture);
	}

	@Test
	public void postForEntity() throws Exception  {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		Future<ResponseEntity<String>> responseEntityFuture =
				template.postForEntity(baseUrl + "/{method}", requestEntity, String.class, "post");
		ResponseEntity<String> responseEntity = responseEntityFuture.get();
		assertEquals("Invalid content", helloWorld, responseEntity.getBody());
	}

	@Test
	public void postForEntityCallback() throws Exception  {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		ListenableFuture<ResponseEntity<String>> responseEntityFuture =
				template.postForEntity(baseUrl + "/{method}", requestEntity, String.class, "post");
		responseEntityFuture.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
			@Override
			public void onSuccess(ResponseEntity<String> result) {
				assertEquals("Invalid content", helloWorld, result.getBody());
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
				result -> assertEquals("Invalid content", helloWorld, result.getBody()),
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
				assertNull(result);
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
				assertNull(result);
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
		deletedFuture.addCallback(Assert::assertNull, ex -> fail(ex.getMessage()));
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
				assertTrue(ex instanceof HttpClientErrorException);
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
			assertTrue(cause instanceof HttpClientErrorException);
			latch.await(5, TimeUnit.SECONDS);
			assertSame(callbackException[0], cause);
		}
	}

	@Test
	public void notFoundGet() throws Exception {
		try {
			Future<?> future = template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null);
			future.get();
			fail("HttpClientErrorException expected");
		}
		catch (ExecutionException ex) {
			assertTrue(ex.getCause() instanceof HttpClientErrorException);
			HttpClientErrorException cause = (HttpClientErrorException)ex.getCause();

			assertEquals(HttpStatus.NOT_FOUND, cause.getStatusCode());
			assertNotNull(cause.getStatusText());
			assertNotNull(cause.getResponseBodyAsString());
		}
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
				assertTrue(t instanceof HttpClientErrorException);
				HttpClientErrorException ex = (HttpClientErrorException) t;
				assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
				assertNotNull(ex.getStatusText());
				assertNotNull(ex.getResponseBodyAsString());
			}
		});
		waitTillDone(future);
	}

	@Test
	public void notFoundCallbackWithLambdas() throws Exception {
		ListenableFuture<?> future = template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null);
		future.addCallback(result -> fail("onSuccess not expected"), ex -> {
				assertTrue(ex instanceof HttpClientErrorException);
				HttpClientErrorException hcex = (HttpClientErrorException) ex;
				assertEquals(HttpStatus.NOT_FOUND, hcex.getStatusCode());
				assertNotNull(hcex.getStatusText());
				assertNotNull(hcex.getResponseBodyAsString());
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
			assertTrue(ex.getCause() instanceof HttpServerErrorException);
			HttpServerErrorException cause = (HttpServerErrorException)ex.getCause();

			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, cause.getStatusCode());
			assertNotNull(cause.getStatusText());
			assertNotNull(cause.getResponseBodyAsString());
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
				assertTrue(ex instanceof HttpServerErrorException);
				HttpServerErrorException hsex = (HttpServerErrorException) ex;
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, hsex.getStatusCode());
				assertNotNull(hsex.getStatusText());
				assertNotNull(hsex.getResponseBodyAsString());
			}
		});
		waitTillDone(future);
	}

	@Test
	public void serverErrorCallbackWithLambdas() throws Exception {
		ListenableFuture<Void> future = template.execute(baseUrl + "/status/server", HttpMethod.GET, null, null);
		future.addCallback(result -> fail("onSuccess not expected"), ex -> {
				assertTrue(ex instanceof HttpServerErrorException);
				HttpServerErrorException hsex = (HttpServerErrorException) ex;
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, hsex.getStatusCode());
				assertNotNull(hsex.getStatusText());
				assertNotNull(hsex.getResponseBodyAsString());
		});
		waitTillDone(future);
	}

	@Test
	public void optionsForAllow() throws Exception {
		Future<Set<HttpMethod>> allowedFuture = template.optionsForAllow(new URI(baseUrl + "/get"));
		Set<HttpMethod> allowed = allowedFuture.get();
		assertEquals("Invalid response",
				EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE), allowed);
	}

	@Test
	public void optionsForAllowCallback() throws Exception {
		ListenableFuture<Set<HttpMethod>> allowedFuture = template.optionsForAllow(new URI(baseUrl + "/get"));
		allowedFuture.addCallback(new ListenableFutureCallback<Set<HttpMethod>>() {
			@Override
			public void onSuccess(Set<HttpMethod> result) {
				assertEquals("Invalid response", EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS,
						HttpMethod.HEAD, HttpMethod.TRACE), result);
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
		allowedFuture.addCallback(result -> assertEquals("Invalid response",
				EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD,HttpMethod.TRACE), result),
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
		assertEquals("Invalid content", helloWorld, response.getBody());
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
				assertEquals("Invalid content", helloWorld, result.getBody());
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
		responseFuture.addCallback(result -> assertEquals("Invalid content", helloWorld,
				result.getBody()), ex -> fail(ex.getMessage()));
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
		assertEquals("Invalid location", new URI(baseUrl + "/post/1"),
				result.getHeaders().getLocation());
		assertFalse(result.hasBody());
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
				assertEquals("Invalid location", expected, result.getHeaders().getLocation());
				assertFalse(result.hasBody());
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
			assertEquals("Invalid location", expected, result.getHeaders().getLocation());
			assertFalse(result.hasBody());
			}, ex -> fail(ex.getMessage()));
		waitTillDone(resultFuture);
	}

	@Test
	public void multipart() throws Exception {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("name 1", "value 1");
		parts.add("name 2", "value 2+1");
		parts.add("name 2", "value 2+2");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);

		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(parts);
		Future<URI> future = template.postForLocation(baseUrl + "/multipart", requestBody);
		future.get();
	}

	@Test
	public void getAndInterceptResponse() throws Exception {
		RequestInterceptor interceptor = new RequestInterceptor();
		template.setInterceptors(Collections.singletonList(interceptor));
		ListenableFuture<ResponseEntity<String>> future = template.getForEntity(baseUrl + "/get", String.class);

		interceptor.latch.await(5, TimeUnit.SECONDS);
		assertNotNull(interceptor.response);
		assertEquals(HttpStatus.OK, interceptor.response.getStatusCode());
		assertNull(interceptor.exception);
		assertEquals(helloWorld, future.get().getBody());
	}

	@Test
	public void getAndInterceptError() throws Exception {
		RequestInterceptor interceptor = new RequestInterceptor();
		template.setInterceptors(Collections.singletonList(interceptor));
		template.getForEntity(baseUrl + "/status/notfound", String.class);

		interceptor.latch.await(5, TimeUnit.SECONDS);
		assertNotNull(interceptor.response);
		assertEquals(HttpStatus.NOT_FOUND, interceptor.response.getStatusCode());
		assertNull(interceptor.exception);
	}

	private void waitTillDone(ListenableFuture<?> future) {
		while (!future.isDone()) {
		}
	}


	private static class RequestInterceptor implements AsyncClientHttpRequestInterceptor {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile ClientHttpResponse response;

		private volatile Throwable exception;

		@Override
		public ListenableFuture<ClientHttpResponse> intercept(HttpRequest request, byte[] body,
				AsyncClientHttpRequestExecution execution) throws IOException {

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
