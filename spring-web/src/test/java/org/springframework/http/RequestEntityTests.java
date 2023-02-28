/*
 * Copyright 2002-2022 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link org.springframework.http.RequestEntity}.
 *
 * @author Arjen Poutsma
 * @author Parviz Rozikov
 */
class RequestEntityTests {

	@Test
	void normal() {
		String headerName = "My-Custom-Header";
		String headerValue = "HeaderValue";
		URI url = URI.create("https://example.com");
		Integer entity = 42;

		RequestEntity<Object> requestEntity =
				RequestEntity.method(HttpMethod.GET, url)
						.header(headerName, headerValue).body(entity);

		assertThat(requestEntity).isNotNull();
		assertThat(requestEntity.getMethod()).isEqualTo(HttpMethod.GET);
		assertThat(requestEntity.getHeaders().containsKey(headerName)).isTrue();
		assertThat(requestEntity.getHeaders().getFirst(headerName)).isEqualTo(headerValue);
		assertThat(requestEntity.getBody()).isEqualTo(entity);
	}

	@Test
	void uriVariablesExpansion() {
		URI uri = UriComponentsBuilder.fromUriString("https://example.com/{foo}").buildAndExpand("bar").toUri();
		RequestEntity.get(uri).accept(MediaType.TEXT_PLAIN).build();

		String url = "https://www.{host}.com/{path}";
		String host = "example";
		String path = "foo/bar";
		URI expected = URI.create("https://www.example.com/foo/bar");

		uri = UriComponentsBuilder.fromUriString(url).buildAndExpand(host, path).toUri();
		RequestEntity<?> entity = RequestEntity.get(uri).build();
		assertThat(entity.getUrl()).isEqualTo(expected);

		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("host", host);
		uriVariables.put("path", path);

		uri = UriComponentsBuilder.fromUriString(url).buildAndExpand(uriVariables).toUri();
		entity = RequestEntity.get(uri).build();
		assertThat(entity.getUrl()).isEqualTo(expected);
	}

	@Test
	void uriExpansion() {
		RequestEntity<Void> entity =
				RequestEntity.get("https://www.{host}.com/{path}", "example", "foo/bar").build();

		assertThat(entity).isInstanceOf(RequestEntity.UriTemplateRequestEntity.class);
		RequestEntity.UriTemplateRequestEntity<Void> ext = (RequestEntity.UriTemplateRequestEntity<Void>) entity;

		assertThat(ext.getUriTemplate()).isEqualTo("https://www.{host}.com/{path}");
		assertThat(ext.getVars()).containsExactly("example", "foo/bar");
	}


	@Test
	void get() {
		RequestEntity<Void> requestEntity = RequestEntity.get(URI.create("https://example.com")).accept(
				MediaType.IMAGE_GIF, MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG).build();

		assertThat(requestEntity).isNotNull();
		assertThat(requestEntity.getMethod()).isEqualTo(HttpMethod.GET);
		assertThat(requestEntity.getHeaders().containsKey(HttpHeaders.ACCEPT)).isTrue();
		assertThat(requestEntity.getHeaders().getFirst(HttpHeaders.ACCEPT)).isEqualTo("image/gif, image/jpeg, image/png");
		assertThat(requestEntity.getBody()).isNull();
	}

	@Test
	void headers() {
		MediaType accept = MediaType.TEXT_PLAIN;
		long ifModifiedSince = 12345L;
		String ifNoneMatch = "\"foo\"";
		long contentLength = 67890;
		MediaType contentType = MediaType.TEXT_PLAIN;

		RequestEntity<Void> responseEntity = RequestEntity.post(URI.create("https://example.com")).
				accept(accept).
				acceptCharset(StandardCharsets.UTF_8).
				ifModifiedSince(ifModifiedSince).
				ifNoneMatch(ifNoneMatch).
				contentLength(contentLength).
				contentType(contentType).
				headers(headers -> assertThat(headers).hasSize(6)).
				build();

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getMethod()).isEqualTo(HttpMethod.POST);
		assertThat(responseEntity.getUrl()).isEqualTo(URI.create("https://example.com"));
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertThat(responseHeaders.getFirst(HttpHeaders.ACCEPT)).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(responseHeaders.getFirst(HttpHeaders.ACCEPT_CHARSET)).isEqualTo("utf-8");
		assertThat(responseHeaders.getFirst(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo("Thu, 01 Jan 1970 00:00:12 GMT");
		assertThat(responseHeaders.getFirst(HttpHeaders.IF_NONE_MATCH)).isEqualTo(ifNoneMatch);
		assertThat(responseHeaders.getFirst(HttpHeaders.CONTENT_LENGTH)).isEqualTo(String.valueOf(contentLength));
		assertThat(responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType.toString());

		assertThat(responseEntity.getBody()).isNull();
	}

	@Test
	void methods() {
		URI url = URI.create("https://example.com");

		RequestEntity<?> entity = RequestEntity.get(url).build();
		assertThat(entity.getMethod()).isEqualTo(HttpMethod.GET);

		entity = RequestEntity.post(url).build();
		assertThat(entity.getMethod()).isEqualTo(HttpMethod.POST);

		entity = RequestEntity.head(url).build();
		assertThat(entity.getMethod()).isEqualTo(HttpMethod.HEAD);

		entity = RequestEntity.options(url).build();
		assertThat(entity.getMethod()).isEqualTo(HttpMethod.OPTIONS);

		entity = RequestEntity.put(url).build();
		assertThat(entity.getMethod()).isEqualTo(HttpMethod.PUT);

		entity = RequestEntity.patch(url).build();
		assertThat(entity.getMethod()).isEqualTo(HttpMethod.PATCH);

		entity = RequestEntity.delete(url).build();
		assertThat(entity.getMethod()).isEqualTo(HttpMethod.DELETE);

	}

	@Test  // SPR-13154
	void types() {
		URI url = URI.create("https://example.com");
		List<String> body = Arrays.asList("foo", "bar");
		ParameterizedTypeReference<?> typeReference = new ParameterizedTypeReference<List<String>>() {};

		RequestEntity<?> entity = RequestEntity.post(url).body(body, typeReference.getType());
		assertThat(entity.getType()).isEqualTo(typeReference.getType());
	}

	@Test
	void equalityWithUrl() {
		RequestEntity<Void> requestEntity1 = RequestEntity.method(HttpMethod.GET, "http://test.api/path/").build();
		RequestEntity<Void> requestEntity2 = RequestEntity.method(HttpMethod.GET, "http://test.api/path/").build();
		RequestEntity<Void> requestEntity3 = RequestEntity.method(HttpMethod.GET, "http://test.api/pathX/").build();

		assertThat(requestEntity1).isEqualTo(requestEntity2);
		assertThat(requestEntity2).isEqualTo(requestEntity1);
		assertThat(requestEntity1).isNotEqualTo(requestEntity3);
		assertThat(requestEntity3).isNotEqualTo(requestEntity2);
		assertThat(requestEntity1.hashCode()).isEqualTo(requestEntity2.hashCode());
		assertThat(requestEntity1.hashCode()).isNotEqualTo(requestEntity3.hashCode());
	}

	@Test  // gh-27531
	void equalityWithUriTemplate() {
		Map<String, Object> vars = Collections.singletonMap("id", "1");

		RequestEntity<Void> requestEntity1 =
				RequestEntity.method(HttpMethod.GET, "http://test.api/path/{id}", vars).build();
		RequestEntity<Void> requestEntity2 =
				RequestEntity.method(HttpMethod.GET, "http://test.api/path/{id}", vars).build();
		RequestEntity<Void> requestEntity3 =
				RequestEntity.method(HttpMethod.GET, "http://test.api/pathX/{id}", vars).build();

		assertThat(requestEntity1).isEqualTo(requestEntity2);
		assertThat(requestEntity2).isEqualTo(requestEntity1);
		assertThat(requestEntity1).isNotEqualTo(requestEntity3);
		assertThat(requestEntity3).isNotEqualTo(requestEntity2);
		assertThat(requestEntity1.hashCode()).isEqualTo(requestEntity2.hashCode());
		assertThat(requestEntity1.hashCode()).isNotEqualTo(requestEntity3.hashCode());
	}

}
