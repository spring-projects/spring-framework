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

package org.springframework.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;
import static org.springframework.http.ResponseEntityBuilder.*;

public class ResponseEntityBuilderTests {

	@Test
	public void normal() {
		String headerName = "My-Custom-Header";
		String headerValue1 = "HeaderValue1";
		String headerValue2 = "HeaderValue2";
		Object entity = new Object();

		ResponseEntity<Object> responseEntity =
				status(HttpStatus.OK).header(headerName, headerValue1, headerValue2).body(entity);

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
		ResponseEntity<?> responseEntity = ok().build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertNull(responseEntity.getBody());
	}

	@Test
	public void okEntity() {
		Object entity = new Object();
		ResponseEntity<Object> responseEntity = ok(entity).build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(entity, responseEntity.getBody());
	}

	@Test
	public void createdLocation() throws URISyntaxException {
		URI location = new URI("location");
		ResponseEntity<?> responseEntity = created(location).build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
		assertTrue(responseEntity.getHeaders().containsKey("Location"));
		assertEquals(location.toString(),
				responseEntity.getHeaders().getFirst("Location"));
		assertNull(responseEntity.getBody());
	}

	@Test
	public void acceptedNoBody() throws URISyntaxException {
		ResponseEntity<?> responseEntity = accepted().build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
		assertNull(responseEntity.getBody());
	}

	@Test
	public void noContent() throws URISyntaxException {
		ResponseEntity<?> responseEntity = ResponseEntityBuilder.noContent().build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
		assertNull(responseEntity.getBody());
	}

	@Test
	public void headers() throws URISyntaxException {
		String eTag = "\"foo\"";
		URI location = new URI("location");
		long contentLength = 67890;
		MediaType contentType = MediaType.TEXT_PLAIN;

		ResponseEntity<?> responseEntity = ResponseEntityBuilder.ok().
				allow(Collections.singleton(HttpMethod.GET)).
				eTag(eTag).
				lastModified(12345L).
				location(location).
				contentLength(contentLength).
				contentType(contentType).
				build();

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertEquals("GET", responseHeaders.getFirst("Allow"));
		assertEquals(eTag, responseHeaders.getFirst("ETag"));
		assertEquals("Thu, 01 Jan 1970 00:00:12 GMT",
				responseHeaders.getFirst("Last-Modified"));
		assertEquals(location.toASCIIString(),
				responseHeaders.getFirst("Location"));
		assertEquals(String.valueOf(contentLength), responseHeaders.getFirst("Content-Length"));
		assertEquals(contentType.toString(), responseHeaders.getFirst("Content-Type"));

		assertNull(responseEntity.getBody());
	}


}