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

package org.springframework.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.UriTemplate;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.springframework.http.RequestEntity}.
 *
 * @author Arjen Poutsma
 */
public class RequestEntityTests {

	@Test
	public void normal() throws URISyntaxException {
		String headerName = "My-Custom-Header";
		String headerValue = "HeaderValue";
		URI url = new URI("http://example.com");
		Integer entity = 42;

		RequestEntity<Object> requestEntity =
				RequestEntity.method(HttpMethod.GET, url)
						.header(headerName, headerValue).body(entity);

		assertNotNull(requestEntity);
		assertEquals(HttpMethod.GET, requestEntity.getMethod());
		assertTrue(requestEntity.getHeaders().containsKey(headerName));
		assertEquals(headerValue, requestEntity.getHeaders().getFirst(headerName));
		assertEquals(entity, requestEntity.getBody());
	}

	@Test
	public void uriVariablesExpansion() throws URISyntaxException {
		URI uri = new UriTemplate("http://example.com/{foo}").expand("bar");
		RequestEntity.get(uri).accept(MediaType.TEXT_PLAIN).build();

		String url = "http://www.{host}.com/{path}";
		String host = "example";
		String path = "foo/bar";
		URI expected = new URI("http://www.example.com/foo/bar");

		uri = new UriTemplate(url).expand(host, path);
		RequestEntity<?> entity = RequestEntity.get(uri).build();
		assertEquals(expected, entity.getUrl());

		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("host", host);
		uriVariables.put("path", path);

		uri = new UriTemplate(url).expand(uriVariables);
		entity = RequestEntity.get(uri).build();
		assertEquals(expected, entity.getUrl());
	}

	@Test
	public void get() {
		RequestEntity<Void> requestEntity = RequestEntity.get(URI.create("http://example.com")).accept(
				MediaType.IMAGE_GIF, MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG).build();

		assertNotNull(requestEntity);
		assertEquals(HttpMethod.GET, requestEntity.getMethod());
		assertTrue(requestEntity.getHeaders().containsKey("Accept"));
		assertEquals("image/gif, image/jpeg, image/png", requestEntity.getHeaders().getFirst("Accept"));
		assertNull(requestEntity.getBody());
	}

	@Test
	public void headers() throws URISyntaxException {
		MediaType accept = MediaType.TEXT_PLAIN;
		Charset charset = Charset.forName("UTF-8");
		long ifModifiedSince = 12345L;
		String ifNoneMatch = "\"foo\"";
		long contentLength = 67890;
		MediaType contentType = MediaType.TEXT_PLAIN;

		RequestEntity<Void> responseEntity = RequestEntity.post(new URI("http://example.com")).
				accept(accept).
				acceptCharset(charset).
				ifModifiedSince(ifModifiedSince).
				ifNoneMatch(ifNoneMatch).
				contentLength(contentLength).
				contentType(contentType).
				build();

		assertNotNull(responseEntity);
		assertEquals(HttpMethod.POST, responseEntity.getMethod());
		assertEquals(new URI("http://example.com"), responseEntity.getUrl());
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertEquals("text/plain", responseHeaders.getFirst("Accept"));
		assertEquals("utf-8", responseHeaders.getFirst("Accept-Charset"));
		assertEquals("Thu, 01 Jan 1970 00:00:12 GMT", responseHeaders.getFirst("If-Modified-Since"));
		assertEquals(ifNoneMatch, responseHeaders.getFirst("If-None-Match"));
		assertEquals(String.valueOf(contentLength), responseHeaders.getFirst("Content-Length"));
		assertEquals(contentType.toString(), responseHeaders.getFirst("Content-Type"));

		assertNull(responseEntity.getBody());
	}

	@Test
	public void methods() throws URISyntaxException {
		URI url = new URI("http://example.com");

		RequestEntity<?> entity = RequestEntity.get(url).build();
		assertEquals(HttpMethod.GET, entity.getMethod());

		entity = RequestEntity.post(url).build();
		assertEquals(HttpMethod.POST, entity.getMethod());

		entity = RequestEntity.head(url).build();
		assertEquals(HttpMethod.HEAD, entity.getMethod());

		entity = RequestEntity.options(url).build();
		assertEquals(HttpMethod.OPTIONS, entity.getMethod());

		entity = RequestEntity.put(url).build();
		assertEquals(HttpMethod.PUT, entity.getMethod());

		entity = RequestEntity.patch(url).build();
		assertEquals(HttpMethod.PATCH, entity.getMethod());

		entity = RequestEntity.delete(url).build();
		assertEquals(HttpMethod.DELETE, entity.getMethod());

	}

	@Test  // SPR-13154
	public void types() throws URISyntaxException {
		URI url = new URI("http://example.com");
		List<String> body = Arrays.asList("foo", "bar");
		ParameterizedTypeReference<?> typeReference = new ParameterizedTypeReference<List<String>>() {};

		RequestEntity<?> entity = RequestEntity.post(url).body(body, typeReference.getType());
		assertEquals(typeReference.getType(), entity.getType());
	}

}
