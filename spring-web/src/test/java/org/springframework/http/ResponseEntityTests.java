/*
 * Copyright 2002-2020 the original author or authors.
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
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getHeaders().containsKey(headerName)).isTrue();
		List<String> list = responseEntity.getHeaders().get(headerName);
		assertThat(list.size()).isEqualTo(2);
		assertThat(list.get(0)).isEqualTo(headerValue1);
		assertThat(list.get(1)).isEqualTo(headerValue2);
		assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
	}

	@Test
	public void okNoBody() {
		ResponseEntity<Void> responseEntity = ResponseEntity.ok().build();

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody()).isNull();
	}

	@Test
	public void okEntity() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.ok(entity);

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
	}

	@Test
	public void ofOptional() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.of(Optional.of(entity));

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
	}

	@Test
	public void ofEmptyOptional() {
		ResponseEntity<Integer> responseEntity = ResponseEntity.of(Optional.empty());

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(responseEntity.getBody()).isNull();
	}

	@Test
	public void createdLocation() throws URISyntaxException {
		URI location = new URI("location");
		ResponseEntity<Void> responseEntity = ResponseEntity.created(location).build();

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.LOCATION)).isTrue();
		assertThat(responseEntity.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo(location.toString());
		assertThat(responseEntity.getBody()).isNull();

		ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
	}

	@Test
	public void acceptedNoBody() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.accepted().build();

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(responseEntity.getBody()).isNull();
	}

	@Test // SPR-14939
	public void acceptedNoBodyWithAlternativeBodyType() throws URISyntaxException {
		ResponseEntity<String> responseEntity = ResponseEntity.accepted().build();

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(responseEntity.getBody()).isNull();
	}

	@Test
	public void noContent() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.noContent().build();

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(responseEntity.getBody()).isNull();
	}

	@Test
	public void badRequest() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.badRequest().build();

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(responseEntity.getBody()).isNull();
	}

	@Test
	public void notFound() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.notFound().build();

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(responseEntity.getBody()).isNull();
	}

	@Test
	public void unprocessableEntity() throws URISyntaxException {
		ResponseEntity<String> responseEntity = ResponseEntity.unprocessableEntity().body("error");

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(responseEntity.getBody()).isEqualTo("error");
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
				headers(headers -> assertThat(headers).hasSize(5)).
				build();

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertThat(responseHeaders.getFirst(HttpHeaders.ALLOW)).isEqualTo(HttpMethod.GET.name());
		assertThat(responseHeaders.getFirst(HttpHeaders.LAST_MODIFIED)).isEqualTo("Thu, 01 Jan 1970 00:00:12 GMT");
		assertThat(responseHeaders.getFirst(HttpHeaders.LOCATION)).isEqualTo(location.toASCIIString());
		assertThat(responseHeaders.getFirst(HttpHeaders.CONTENT_LENGTH)).isEqualTo(String.valueOf(contentLength));
		assertThat(responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType.toString());

		assertThat((Object) responseEntity.getBody()).isNull();
	}

	@Test
	public void Etagheader() throws URISyntaxException {

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().eTag("\"foo\"").build();
		assertThat(responseEntity.getHeaders().getETag()).isEqualTo("\"foo\"");

		responseEntity = ResponseEntity.ok().eTag("foo").build();
		assertThat(responseEntity.getHeaders().getETag()).isEqualTo("\"foo\"");

		responseEntity = ResponseEntity.ok().eTag("W/\"foo\"").build();
		assertThat(responseEntity.getHeaders().getETag()).isEqualTo("W/\"foo\"");
	}

	@Test
	public void headersCopy() {
		HttpHeaders customHeaders = new HttpHeaders();
		customHeaders.set("X-CustomHeader", "vale");

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().headers(customHeaders).build();
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseHeaders.size()).isEqualTo(1);
		assertThat(responseHeaders.get("X-CustomHeader").size()).isEqualTo(1);
		assertThat(responseHeaders.getFirst("X-CustomHeader")).isEqualTo("vale");

	}

	@Test  // SPR-12792
	public void headersCopyWithEmptyAndNull() {
		ResponseEntity<Void> responseEntityWithEmptyHeaders =
				ResponseEntity.ok().headers(new HttpHeaders()).build();
		ResponseEntity<Void> responseEntityWithNullHeaders =
				ResponseEntity.ok().headers((HttpHeaders) null).build();

		assertThat(responseEntityWithEmptyHeaders.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntityWithEmptyHeaders.getHeaders().isEmpty()).isTrue();
		assertThat(responseEntityWithNullHeaders.toString()).isEqualTo(responseEntityWithEmptyHeaders.toString());
	}

	@Test
	public void emptyCacheControl() {
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.empty())
						.body(entity);

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)).isFalse();
		assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
	}

	@Test
	public void cacheControl() {
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate().
								mustRevalidate().proxyRevalidate().sMaxAge(30, TimeUnit.MINUTES))
						.body(entity);

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)).isTrue();
		assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
		String cacheControlHeader = responseEntity.getHeaders().getCacheControl();
		assertThat(cacheControlHeader).isEqualTo(
				"max-age=3600, must-revalidate, private, proxy-revalidate, s-maxage=1800");
	}

	@Test
	public void cacheControlNoCache() {
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.noStore())
						.body(entity);

		assertThat(responseEntity).isNotNull();
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)).isTrue();
		assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);

		String cacheControlHeader = responseEntity.getHeaders().getCacheControl();
		assertThat(cacheControlHeader).isEqualTo("no-store");
	}

	@Test
	public void statusCodeAsInt() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.status(200).body(entity);

		assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);
		assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
	}

	@Test
	public void customStatusCode() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.status(299).body(entity);

		assertThat(responseEntity.getStatusCodeValue()).isEqualTo(299);
		assertThat((int) responseEntity.getBody()).isEqualTo((int) entity);
	}

}
