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

package org.springframework.http;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class HttpEntityTests {

	@Test
	public void noHeaders() {
		String body = "foo";
		HttpEntity<String> entity = new HttpEntity<>(body);
		assertThat(entity.getBody()).isSameAs(body);
		assertThat(entity.getHeaders().isEmpty()).isTrue();
	}

	@Test
	public void httpHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		String body = "foo";
		HttpEntity<String> entity = new HttpEntity<>(body, headers);
		assertThat(entity.getBody()).isEqualTo(body);
		assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(entity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");
	}

	@Test
	public void multiValueMap() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.set("Content-Type", "text/plain");
		String body = "foo";
		HttpEntity<String> entity = new HttpEntity<>(body, map);
		assertThat(entity.getBody()).isEqualTo(body);
		assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(entity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");
	}

	@Test
	public void testEquals() {
		MultiValueMap<String, String> map1 = new LinkedMultiValueMap<>();
		map1.set("Content-Type", "text/plain");

		MultiValueMap<String, String> map2 = new LinkedMultiValueMap<>();
		map2.set("Content-Type", "application/json");

		assertThat(new HttpEntity<>().equals(new HttpEntity<Object>())).isTrue();
		assertThat(new HttpEntity<>(map1).equals(new HttpEntity<Object>())).isFalse();
		assertThat(new HttpEntity<>().equals(new HttpEntity<Object>(map2))).isFalse();

		assertThat(new HttpEntity<>(map1).equals(new HttpEntity<Object>(map1))).isTrue();
		assertThat(new HttpEntity<>(map1).equals(new HttpEntity<Object>(map2))).isFalse();

		assertThat(new HttpEntity<String>(null, null).equals(new HttpEntity<String>(null, null))).isTrue();
		assertThat(new HttpEntity<>("foo", null).equals(new HttpEntity<String>(null, null))).isFalse();
		assertThat(new HttpEntity<String>(null, null).equals(new HttpEntity<>("bar", null))).isFalse();

		assertThat(new HttpEntity<>("foo", map1).equals(new HttpEntity<String>("foo", map1))).isTrue();
		assertThat(new HttpEntity<>("foo", map1).equals(new HttpEntity<String>("bar", map1))).isFalse();
	}

	@Test
	public void responseEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		String body = "foo";
		HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> responseEntity = new ResponseEntity<>(body, headers, HttpStatus.OK);
		ResponseEntity<String> responseEntity2 = new ResponseEntity<>(body, headers, HttpStatus.OK);

		assertThat(responseEntity.getBody()).isEqualTo(body);
		assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(responseEntity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");
		assertThat(responseEntity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");

		assertThat(httpEntity.equals(responseEntity)).isFalse();
		assertThat(responseEntity.equals(httpEntity)).isFalse();
		assertThat(responseEntity.equals(responseEntity2)).isTrue();
		assertThat(responseEntity2.equals(responseEntity)).isTrue();
	}

	@Test
	public void requestEntity() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		String body = "foo";
		HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
		RequestEntity<String> requestEntity = new RequestEntity<>(body, headers, HttpMethod.GET, new URI("/"));
		RequestEntity<String> requestEntity2 = new RequestEntity<>(body, headers, HttpMethod.GET, new URI("/"));

		assertThat(requestEntity.getBody()).isEqualTo(body);
		assertThat(requestEntity.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(requestEntity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");
		assertThat(requestEntity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");

		assertThat(httpEntity.equals(requestEntity)).isFalse();
		assertThat(requestEntity.equals(httpEntity)).isFalse();
		assertThat(requestEntity.equals(requestEntity2)).isTrue();
		assertThat(requestEntity2.equals(requestEntity)).isTrue();
	}

}
