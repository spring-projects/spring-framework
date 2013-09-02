/*
 * Copyright 2002-2013 the original author or authors.
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

/** @author Arjen Poutsma */
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
	public void postForLocation()
			throws URISyntaxException, ExecutionException, InterruptedException {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		Future<URI> locationFuture = template.postForLocation(baseUrl + "/{method}", requestEntity,
				"post");
		URI location = locationFuture.get();
		assertEquals("Invalid location", new URI(baseUrl + "/post/1"), location);
	}

	@Test
	public void postForLocationEntity()
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
	public void put()
			throws URISyntaxException, ExecutionException, InterruptedException {
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld);
		Future<?>
				responseEntityFuture = template.put(baseUrl + "/{method}", requestEntity,
				"put");
		responseEntityFuture.get();
	}

	@Test
	public void delete()
			throws URISyntaxException, ExecutionException, InterruptedException {
		Future<?> deletedFuture = template.delete(new URI(baseUrl + "/delete"));
		deletedFuture.get();
	}

	@Test
	public void notFound() throws ExecutionException, InterruptedException {
		try {
			Future<Void> future = template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null);
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
	public void optionsForAllow()
			throws URISyntaxException, ExecutionException, InterruptedException {
		Future<Set<HttpMethod>>
				allowedFuture = template.optionsForAllow(new URI(baseUrl + "/get"));
		Set<HttpMethod> allowed = allowedFuture.get();
		assertEquals("Invalid response",
				EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE), allowed);
	}

	@Test
	@SuppressWarnings("unchecked")
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
