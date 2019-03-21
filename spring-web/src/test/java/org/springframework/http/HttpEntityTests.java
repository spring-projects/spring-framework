/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http;

import java.net.URI;

import org.junit.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class HttpEntityTests {

	@Test
	public void noHeaders() {
		String body = "foo";
		HttpEntity<String> entity = new HttpEntity<>(body);
		assertSame(body, entity.getBody());
		assertTrue(entity.getHeaders().isEmpty());
	}

	@Test
	public void httpHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		String body = "foo";
		HttpEntity<String> entity = new HttpEntity<>(body, headers);
		assertEquals(body, entity.getBody());
		assertEquals(MediaType.TEXT_PLAIN, entity.getHeaders().getContentType());
		assertEquals("text/plain", entity.getHeaders().getFirst("Content-Type"));
	}

	@Test
	public void multiValueMap() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.set("Content-Type", "text/plain");
		String body = "foo";
		HttpEntity<String> entity = new HttpEntity<>(body, map);
		assertEquals(body, entity.getBody());
		assertEquals(MediaType.TEXT_PLAIN, entity.getHeaders().getContentType());
		assertEquals("text/plain", entity.getHeaders().getFirst("Content-Type"));
	}

	@Test
	public void testEquals() {
		MultiValueMap<String, String> map1 = new LinkedMultiValueMap<>();
		map1.set("Content-Type", "text/plain");

		MultiValueMap<String, String> map2 = new LinkedMultiValueMap<>();
		map2.set("Content-Type", "application/json");

		assertTrue(new HttpEntity<>().equals(new HttpEntity<Object>()));
		assertFalse(new HttpEntity<>(map1).equals(new HttpEntity<Object>()));
		assertFalse(new HttpEntity<>().equals(new HttpEntity<Object>(map2)));

		assertTrue(new HttpEntity<>(map1).equals(new HttpEntity<Object>(map1)));
		assertFalse(new HttpEntity<>(map1).equals(new HttpEntity<Object>(map2)));

		assertTrue(new HttpEntity<String>(null, null).equals(new HttpEntity<String>(null, null)));
		assertFalse(new HttpEntity<>("foo", null).equals(new HttpEntity<String>(null, null)));
		assertFalse(new HttpEntity<String>(null, null).equals(new HttpEntity<>("bar", null)));

		assertTrue(new HttpEntity<>("foo", map1).equals(new HttpEntity<String>("foo", map1)));
		assertFalse(new HttpEntity<>("foo", map1).equals(new HttpEntity<String>("bar", map1)));
	}

	@Test
	public void responseEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		String body = "foo";
		HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> responseEntity = new ResponseEntity<>(body, headers, HttpStatus.OK);
		ResponseEntity<String> responseEntity2 = new ResponseEntity<>(body, headers, HttpStatus.OK);

		assertEquals(body, responseEntity.getBody());
		assertEquals(MediaType.TEXT_PLAIN, responseEntity.getHeaders().getContentType());
		assertEquals("text/plain", responseEntity.getHeaders().getFirst("Content-Type"));
		assertEquals("text/plain", responseEntity.getHeaders().getFirst("Content-Type"));

		assertFalse(httpEntity.equals(responseEntity));
		assertFalse(responseEntity.equals(httpEntity));
		assertTrue(responseEntity.equals(responseEntity2));
		assertTrue(responseEntity2.equals(responseEntity));
	}

	@Test
	public void requestEntity() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		String body = "foo";
		HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
		RequestEntity<String> requestEntity = new RequestEntity<>(body, headers, HttpMethod.GET, new URI("/"));
		RequestEntity<String> requestEntity2 = new RequestEntity<>(body, headers, HttpMethod.GET, new URI("/"));

		assertEquals(body, requestEntity.getBody());
		assertEquals(MediaType.TEXT_PLAIN, requestEntity.getHeaders().getContentType());
		assertEquals("text/plain", requestEntity.getHeaders().getFirst("Content-Type"));
		assertEquals("text/plain", requestEntity.getHeaders().getFirst("Content-Type"));

		assertFalse(httpEntity.equals(requestEntity));
		assertFalse(requestEntity.equals(httpEntity));
		assertTrue(requestEntity.equals(requestEntity2));
		assertTrue(requestEntity2.equals(requestEntity));
	}

}
