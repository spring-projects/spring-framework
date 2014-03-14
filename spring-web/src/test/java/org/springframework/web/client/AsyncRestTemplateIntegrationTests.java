/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * @author Arjen Poutsma
 */
public class AsyncRestTemplateIntegrationTests extends AbstractJettyServerTestCase {

	private AsyncRestTemplate template;

	@Before
	public void createTemplate() {
		template = new AsyncRestTemplate(
				new HttpComponentsAsyncClientHttpRequestFactory());
	}

	@Test
	public void getEntity() throws ExecutionException, InterruptedException {
		Future<ResponseEntity<String>>
				futureEntity = template.getForEntity(baseUrl + "/{method}", String.class, "get");
		ResponseEntity<String> entity = futureEntity.get();
		assertEquals("Invalid content", helloWorld, entity.getBody());
		assertFalse("No headers", entity.getHeaders().isEmpty());
		assertEquals("Invalid content-type", contentType, entity.getHeaders().getContentType());
		assertEquals("Invalid status code", HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void multipleFutureGets() throws ExecutionException, InterruptedException {
		Future<ResponseEntity<String>>
				futureEntity = template.getForEntity(baseUrl + "/{method}", String.class, "get");
		futureEntity.get();
		futureEntity.get();
	}

	@Test
	public void getEntityCallback() throws ExecutionException, InterruptedException {
		ListenableFuture<ResponseEntity<String>>
				futureEntity = template.getForEntity(baseUrl + "/{method}", String.class, "get");
		futureEntity.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
			@Override
			public void onSuccess(ResponseEntity<String> entity) {
				assertEquals("Invalid content", helloWorld, entity.getBody());
				assertFalse("No headers", entity.getHeaders().isEmpty());
				assertEquals("Invalid content-type", contentType, entity.getHeaders().getContentType());
				assertEquals("Invalid status code", HttpStatus.OK, entity.getStatusCode());
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		// wait till done
		while (!futureEntity.isDone()) {
		}
	}

	@Test
	public void getNoResponse() throws ExecutionException, InterruptedException {
		Future<ResponseEntity<String>>
				futureEntity = template.getForEntity(baseUrl + "/get/nothing", String.class);
		ResponseEntity<String> entity = futureEntity.get();
		assertNull("Invalid content", entity.getBody());
	}


	@Test
	public void getNoContentTypeHeader()
			throws UnsupportedEncodingException, ExecutionException,
			InterruptedException {
		Future<ResponseEntity<byte[]>>
				futureEntity = template.getForEntity(baseUrl + "/get/nocontenttype",
				byte[].class);
		ResponseEntity<byte[]> responseEntity = futureEntity.get();
		assertArrayEquals("Invalid content", helloWorld.getBytes("UTF-8"),
				responseEntity.getBody());
	}


	@Test
	public void getNoContent() throws ExecutionException, InterruptedException {
		Future<ResponseEntity<String>>
				responseFuture = template.getForEntity(baseUrl + "/status/nocontent", String.class);
		ResponseEntity<String> entity = responseFuture.get();
		assertEquals("Invalid response code", HttpStatus.NO_CONTENT, entity.getStatusCode());
		assertNull("Invalid content", entity.getBody());
	}

	@Test
	public void getNotModified() throws ExecutionException, InterruptedException {
		Future<ResponseEntity<String>>
				responseFuture = template.getForEntity(baseUrl + "/status/notmodified",
				String.class);
		ResponseEntity<String> entity = responseFuture.get();
		assertEquals("Invalid response code", HttpStatus.NOT_MODIFIED, entity.getStatusCode());
		assertNull("Invalid content", entity.getBody());
	}

	@Test
	public void headForHeaders() throws ExecutionException, InterruptedException {
		Future<HttpHeaders> headersFuture = template.headForHeaders(baseUrl + "/get");
		HttpHeaders headers = headersFuture.get();
		assertTrue("No Content-Type header", headers.containsKey("Content-Type"));
	}

	@Test
	public void headForHeadersCallback() throws ExecutionException, InterruptedException {
		ListenableFuture<HttpHeaders> headersFuture = template.headForHeaders(baseUrl + "/get");
		headersFuture.addCallback(new ListenableFutureCallback<HttpHeaders>() {
			@Override
			public void onSuccess(HttpHeaders result) {
				assertTrue("No Content-Type header", result.containsKey("Content-Type"));
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		while (!headersFuture.isDone()) {
		}
	}

	@Test
	public void postForLocation()
			throws URISyntaxException, ExecutionException, InterruptedException {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", Charset.forName("ISO-8859-15")));
		HttpEntity<String> entity = new HttpEntity<String>(helloWorld, entityHeaders);
		Future<URI>
				locationFuture = template.postForLocation(baseUrl + "/{method}", entity,
				"post");
		URI location = locationFuture.get();
		assertEquals("Invalid location", new URI(baseUrl + "/post/1"), location);
	}

	@Test
	public void postForLocationCallback()
			throws URISyntaxException, ExecutionException, InterruptedException {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", Charset.forName("ISO-8859-15")));
		HttpEntity<String> entity = new HttpEntity<String>(helloWorld, entityHeaders);
		final URI expected = new URI(baseUrl + "/post/1");
		ListenableFuture<URI>
				locationFuture = template.postForLocation(baseUrl + "/{method}", entity,
				"post");
		locationFuture.addCallback(new ListenableFutureCallback<URI>() {
			@Override
			public void onSuccess(URI result) {
				assertEquals("Invalid location", expected, result);
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		while (!locationFuture.isDone()) {
		}
	}

	@Test
	public void postForEntity()
			throws URISyntaxException, ExecutionException, InterruptedException {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		Future<ResponseEntity<String>>
				responseEntityFuture = template.postForEntity(baseUrl + "/{method}", requestEntity,
				String.class, "post");
		ResponseEntity<String> responseEntity = responseEntityFuture.get();
		assertEquals("Invalid content", helloWorld, responseEntity.getBody());
	}

	@Test
	public void postForEntityCallback()
			throws URISyntaxException, ExecutionException, InterruptedException {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		ListenableFuture<ResponseEntity<String>>
				responseEntityFuture = template.postForEntity(baseUrl + "/{method}", requestEntity,
				String.class, "post");
		responseEntityFuture.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
			@Override
			public void onSuccess(ResponseEntity<String> result) {
				assertEquals("Invalid content", helloWorld, result.getBody());
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		while (!responseEntityFuture.isDone()) {
		}
	}

	@Test
	public void put()
			throws URISyntaxException, ExecutionException, InterruptedException {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		Future<?>
				responseEntityFuture = template.put(baseUrl + "/{method}", requestEntity,
				"put");
		responseEntityFuture.get();
	}

	@Test
	public void putCallback()
			throws URISyntaxException, ExecutionException, InterruptedException {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		ListenableFuture<?>
				responseEntityFuture = template.put(baseUrl + "/{method}", requestEntity,
				"put");
		responseEntityFuture.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onSuccess(Object result) {
				assertNull(result);
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		while (!responseEntityFuture.isDone()) {
		}
	}

	@Test
	public void delete()
			throws URISyntaxException, ExecutionException, InterruptedException {
		Future<?> deletedFuture = template.delete(new URI(baseUrl + "/delete"));

		deletedFuture.get();
	}

	@Test
	public void deleteCallback()
			throws URISyntaxException, ExecutionException, InterruptedException {
		ListenableFuture<?> deletedFuture = template.delete(new URI(baseUrl + "/delete"));
		deletedFuture.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onSuccess(Object result) {
				assertNull(result);
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		while (!deletedFuture.isDone()) {
		}
	}

	@Test
	public void notFound() throws ExecutionException, InterruptedException {
		try {
			Future<?> future = template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null);
			future.get();
			fail("HttpClientErrorException expected");
		}
		catch (HttpClientErrorException ex) {
			assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
			assertNotNull(ex.getStatusText());
			assertNotNull(ex.getResponseBodyAsString());
		}
	}

	@Test
	public void notFoundCallback() throws ExecutionException, InterruptedException {
		ListenableFuture<?> future =
				template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null,
						null);
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
		while (!future.isDone()) {
		}
	}

	@Test
	public void serverError() throws ExecutionException, InterruptedException {
		try {
			Future<Void> future = template.execute(baseUrl + "/status/server", HttpMethod.GET, null, null);
			future.get();
			fail("HttpServerErrorException expected");
		}
		catch (HttpServerErrorException ex) {
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
			assertNotNull(ex.getStatusText());
			assertNotNull(ex.getResponseBodyAsString());
		}
	}

	@Test
	public void serverErrorCallback() throws ExecutionException, InterruptedException {
		ListenableFuture<Void> future = template.execute(baseUrl + "/status/server", HttpMethod.GET, null, null);
		future.addCallback(new ListenableFutureCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				fail("onSuccess not expected");
			}

			@Override
			public void onFailure(Throwable t) {
				assertTrue(t instanceof HttpServerErrorException);
				HttpServerErrorException ex = (HttpServerErrorException) t;
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
				assertNotNull(ex.getStatusText());
				assertNotNull(ex.getResponseBodyAsString());
			}
		});
		while (!future.isDone()) {
		}
	}

	@Test
	public void optionsForAllow()
			throws URISyntaxException, ExecutionException, InterruptedException {
		Future<Set<HttpMethod>>
				allowedFuture = template.optionsForAllow(new URI(baseUrl + "/get"));
		Set<HttpMethod> allowed = allowedFuture.get();
		assertEquals("Invalid response",
				EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE), allowed);
	}

	@Test
	public void optionsForAllowCallback()
			throws URISyntaxException, ExecutionException, InterruptedException {
		ListenableFuture<Set<HttpMethod>>
				allowedFuture = template.optionsForAllow(new URI(baseUrl + "/get"));
		allowedFuture.addCallback(new ListenableFutureCallback<Set<HttpMethod>>() {
			@Override
			public void onSuccess(Set<HttpMethod> result) {
				assertEquals("Invalid response",
						EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE), result);
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		while (!allowedFuture.isDone()) {
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void exchangeGet() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		HttpEntity<?> requestEntity = new HttpEntity(requestHeaders);
		Future<ResponseEntity<String>> responseFuture =
				template.exchange(baseUrl + "/{method}", HttpMethod.GET, requestEntity,
						String.class, "get");
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
				template.exchange(baseUrl + "/{method}", HttpMethod.GET, requestEntity,
						String.class, "get");
		responseFuture.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
			@Override
			public void onSuccess(ResponseEntity<String> result) {
				assertEquals("Invalid content", helloWorld, result.getBody());
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		while (!responseFuture.isDone()) {
		}
	}

	@Test
	public void exchangePost() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		requestHeaders.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> requestEntity = new HttpEntity<String>(helloWorld, requestHeaders);
		Future<ResponseEntity<Void>>
				resultFuture = template.exchange(baseUrl + "/{method}", HttpMethod.POST,
				requestEntity, Void.class, "post");
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
		HttpEntity<String> requestEntity = new HttpEntity<String>(helloWorld, requestHeaders);
		ListenableFuture<ResponseEntity<Void>>
				resultFuture = template.exchange(baseUrl + "/{method}", HttpMethod.POST,
				requestEntity, Void.class, "post");
		final URI expected =new URI(baseUrl + "/post/1");
		resultFuture.addCallback(new ListenableFutureCallback<ResponseEntity<Void>>() {
			@Override
			public void onSuccess(ResponseEntity<Void> result) {
				assertEquals("Invalid location", expected,
						result.getHeaders().getLocation());
				assertFalse(result.hasBody());
			}

			@Override
			public void onFailure(Throwable t) {
				fail(t.getMessage());
			}
		});
		while (!resultFuture.isDone()) {
		}

	}

	@Test
	public void multipart() throws UnsupportedEncodingException, ExecutionException,
			InterruptedException {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		parts.add("name 1", "value 1");
		parts.add("name 2", "value 2+1");
		parts.add("name 2", "value 2+2");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);

		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(parts);
		Future<URI> future =
				template.postForLocation(baseUrl + "/multipart", requestBody);
		future.get();
	}

}
