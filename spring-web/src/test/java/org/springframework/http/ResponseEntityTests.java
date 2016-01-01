/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Marcel Overdijk
 * @author Kazuki Shimizu
 */
public class ResponseEntityTests {

	@Test
	public void normal() {
		String headerName = "My-Custom-Header";
		String headerValue1 = "HeaderValue1";
		String headerValue2 = "HeaderValue2";
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK).header(headerName, headerValue1, headerValue2).body(entity);

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertTrue(responseEntity.getHeaders().containsKey(headerName));
		List<String> list = responseEntity.getHeaders().get(headerName);
		assertEquals(2, list.size());
		assertEquals(headerValue1, list.get(0));
		assertEquals(headerValue2, list.get(1));
		assertEquals(entity, responseEntity.getBody());
	}

	@Test
	public void okNoBody() {
		ResponseEntity<Void> responseEntity = ResponseEntity.ok().build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertNull(responseEntity.getBody());
	}

	@Test
	public void okEntity() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.ok(entity);

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(entity, responseEntity.getBody());
	}

	@Test
	public void createdLocation() throws URISyntaxException {
		URI location = new URI("location");
		ResponseEntity<Void> responseEntity = ResponseEntity.created(location).build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
		assertTrue(responseEntity.getHeaders().containsKey("Location"));
		assertEquals(location.toString(),
				responseEntity.getHeaders().getFirst("Location"));
		assertNull(responseEntity.getBody());

		ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
	}

	@Test
	public void acceptedNoBody() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.accepted().build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
		assertNull(responseEntity.getBody());
	}

	@Test
	public void noContent() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.noContent().build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
		assertNull(responseEntity.getBody());
	}

	@Test
	public void badRequest() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.badRequest().build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertNull(responseEntity.getBody());
	}

	@Test
	public void notFound() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.notFound().build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
		assertNull(responseEntity.getBody());
	}

	@Test
	public void unprocessableEntity() throws URISyntaxException {
		ResponseEntity<String> responseEntity = ResponseEntity.unprocessableEntity().body("error");

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
		assertEquals("error", responseEntity.getBody());
	}

	@Test
	public void headers() throws URISyntaxException {
		URI location = new URI("location");
		long contentLength = 67890;
		MediaType contentType = MediaType.TEXT_PLAIN;

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().
				allow(HttpMethod.GET).
				lastModified(12345L).
				location(location).
				contentLength(contentLength).
				contentType(contentType).
				build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertEquals("GET", responseHeaders.getFirst("Allow"));
		assertEquals("Thu, 01 Jan 1970 00:00:12 GMT",
				responseHeaders.getFirst("Last-Modified"));
		assertEquals(location.toASCIIString(),
				responseHeaders.getFirst("Location"));
		assertEquals(String.valueOf(contentLength), responseHeaders.getFirst("Content-Length"));
		assertEquals(contentType.toString(), responseHeaders.getFirst("Content-Type"));

		assertNull(responseEntity.getBody());
	}

	@Test
	public void Etagheader() throws URISyntaxException {

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().eTag("\"foo\"").build();
		assertEquals("\"foo\"", responseEntity.getHeaders().getETag());

		responseEntity = ResponseEntity.ok().eTag("foo").build();
		assertEquals("\"foo\"", responseEntity.getHeaders().getETag());

		responseEntity = ResponseEntity.ok().eTag("W/\"foo\"").build();
		assertEquals("W/\"foo\"", responseEntity.getHeaders().getETag());
	}

	@Test
	public void headersCopy() {
		HttpHeaders customHeaders = new HttpHeaders();
		customHeaders.set("X-CustomHeader", "vale");

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().headers(customHeaders).build();
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(1, responseHeaders.size());
		assertEquals(1, responseHeaders.get("X-CustomHeader").size());
		assertEquals("vale", responseHeaders.getFirst("X-CustomHeader"));

	}

	@Test  // SPR-12792
	public void headersCopyWithEmptyAndNull() {
		ResponseEntity<Void> responseEntityWithEmptyHeaders =
				ResponseEntity.ok().headers(new HttpHeaders()).build();
		ResponseEntity<Void> responseEntityWithNullHeaders =
				ResponseEntity.ok().headers(null).build();

		assertEquals(HttpStatus.OK, responseEntityWithEmptyHeaders.getStatusCode());
		assertTrue(responseEntityWithEmptyHeaders.getHeaders().isEmpty());
		assertEquals(responseEntityWithEmptyHeaders.toString(), responseEntityWithNullHeaders.toString());
	}

	@Test
	public void emptyCacheControl() {
		Integer entity = new Integer(42);

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.empty())
						.body(entity);

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertFalse(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL));
		assertEquals(entity, responseEntity.getBody());
	}

	@Test
	public void cacheControl() {
		Integer entity = new Integer(42);

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate().
								mustRevalidate().proxyRevalidate().sMaxAge(30, TimeUnit.MINUTES))
						.body(entity);

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertTrue(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL));
		assertEquals(entity, responseEntity.getBody());
		String cacheControlHeader = responseEntity.getHeaders().getCacheControl();
		assertThat(cacheControlHeader, Matchers.equalTo("max-age=3600, must-revalidate, private, proxy-revalidate, s-maxage=1800"));
	}

	@Test
	public void cacheControlNoCache() {
		Integer entity = new Integer(42);

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.noStore())
						.body(entity);

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertTrue(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL));
		assertEquals(entity, responseEntity.getBody());

		String cacheControlHeader = responseEntity.getHeaders().getCacheControl();
		assertThat(cacheControlHeader, Matchers.equalTo("no-store"));
	}

}
