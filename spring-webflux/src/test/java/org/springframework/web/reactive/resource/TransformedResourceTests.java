/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.resource;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TransformedResource}.
 */
class TransformedResourceTests {

	@Test
	void etagReflectsTransformedContent() throws Exception {
		HttpHeaders originalHeaders = new HttpHeaders();
		originalHeaders.setETag("W/\"original\"");
		Resource original = new TestHttpResource(originalHeaders);
		TransformedResource transformed = new TransformedResource(original, "hello".getBytes(UTF_8));

		assertThat(transformed).isInstanceOf(HttpResource.class);
		assertThat(((HttpResource) transformed).getResponseHeaders().getETag())
				.isEqualTo("W/\"5d41402abc4b2a76b9719d911017c592\"");
		assertThat(originalHeaders.getETag()).isEqualTo("W/\"original\"");
		assertThat(transformed.getFilename()).isEqualTo(original.getFilename());
		assertThat(transformed.lastModified()).isEqualTo(original.lastModified());
		assertThat(transformed.contentLength()).isEqualTo(5);
	}

	@Test
	void noEtagForOriginalWithoutHttpHeaders() {
		Resource original = new ClassPathResource("test/main.css", getClass());
		Resource transformed = new TransformedResource(original, "hello".getBytes(UTF_8));

		assertThat(((HttpResource) transformed).getResponseHeaders().isEmpty()).isTrue();
	}

	@Test
	void noEtagForHttpResourceWithoutEtag() {
		HttpHeaders originalHeaders = new HttpHeaders();
		originalHeaders.setCacheControl("max-age=3600");
		Resource transformed = new TransformedResource(new TestHttpResource(originalHeaders), "hello".getBytes(UTF_8));

		assertThat(((HttpResource) transformed).getResponseHeaders().isEmpty()).isTrue();
	}

	@Test
	void originalHeadersAreNotInherited() {
		HttpHeaders originalHeaders = new HttpHeaders();
		originalHeaders.setETag("W/\"original\"");
		originalHeaders.setContentLength(100);
		originalHeaders.set(HttpHeaders.CONTENT_ENCODING, "gzip");
		originalHeaders.set(HttpHeaders.CONTENT_RANGE, "bytes 0-99/100");
		originalHeaders.add(HttpHeaders.VARY, "Accept-Encoding");
		originalHeaders.add(HttpHeaders.VARY, "Accept-Language");
		HttpHeaders snapshot = HttpHeaders.copyOf(originalHeaders);
		Resource transformed = new TransformedResource(
				new TestHttpResource(HttpHeaders.readOnlyHttpHeaders(originalHeaders)), "hello".getBytes(UTF_8));

		assertThat(((HttpResource) transformed).getResponseHeaders().headerNames()).containsExactly(HttpHeaders.ETAG);
		assertThat(originalHeaders).isEqualTo(snapshot);
	}

	@Test
	void responseHeadersAreIndependentAndMutable() {
		HttpHeaders originalHeaders = new HttpHeaders();
		originalHeaders.setETag("W/\"original\"");
		Resource transformed = new TransformedResource(new TestHttpResource(originalHeaders), "hello".getBytes(UTF_8));

		HttpHeaders first = ((HttpResource) transformed).getResponseHeaders();
		first.setETag("\"changed\"");
		first.add(HttpHeaders.CONTENT_ENCODING, "gzip");

		HttpHeaders second = ((HttpResource) transformed).getResponseHeaders();
		assertThat(second.getETag()).isEqualTo("W/\"5d41402abc4b2a76b9719d911017c592\"");
		assertThat(second.headerNames()).containsExactly(HttpHeaders.ETAG);
		assertThat(originalHeaders.getETag()).isEqualTo("W/\"original\"");
	}

	@Test
	void etagReflectsCurrentByteArray() {
		HttpHeaders originalHeaders = new HttpHeaders();
		originalHeaders.setETag("W/\"original\"");
		TransformedResource transformed = new TransformedResource(
				new TestHttpResource(originalHeaders), "hello".getBytes(UTF_8));
		String firstEtag = ((HttpResource) transformed).getResponseHeaders().getETag();

		System.arraycopy("world".getBytes(UTF_8), 0, transformed.getByteArray(), 0, 5);

		assertThat(((HttpResource) transformed).getResponseHeaders().getETag())
				.isEqualTo("W/\"7d793037a0760186574b0282f2f435e7\"")
				.isNotEqualTo(firstEtag);
	}

	@Test
	void etagReflectsOutermostTransformation() {
		HttpHeaders originalHeaders = new HttpHeaders();
		originalHeaders.setETag("W/\"original\"");
		TransformedResource first = new TransformedResource(
				new TestHttpResource(originalHeaders), "hello".getBytes(UTF_8));
		Resource second = new TransformedResource(first, "world".getBytes(UTF_8));

		assertThat(((HttpResource) second).getResponseHeaders().getETag())
				.isEqualTo("W/\"7d793037a0760186574b0282f2f435e7\"");
	}


	private static class TestHttpResource extends ClassPathResource implements HttpResource {

		private final HttpHeaders headers;


		TestHttpResource(HttpHeaders headers) {
			super("test/main.css", TransformedResourceTests.class);
			this.headers = headers;
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			return this.headers;
		}
	}

}
