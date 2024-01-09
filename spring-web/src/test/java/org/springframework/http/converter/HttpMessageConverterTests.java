/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.converter;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test-case for AbstractHttpMessageConverter.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class HttpMessageConverterTests {


	@Test
	void canRead() {
		MediaType mediaType = new MediaType("foo", "bar");
		HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

		assertThat(converter.canRead(MyType.class, mediaType)).isTrue();
		assertThat(converter.canRead(MyType.class, new MediaType("foo", "*"))).isFalse();
		assertThat(converter.canRead(MyType.class, MediaType.ALL)).isFalse();
	}

	@Test
	void canReadWithWildcardSubtype() {
		MediaType mediaType = new MediaType("foo");
		HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

		assertThat(converter.canRead(MyType.class, new MediaType("foo", "bar"))).isTrue();
		assertThat(converter.canRead(MyType.class, new MediaType("foo", "*"))).isTrue();
		assertThat(converter.canRead(MyType.class, MediaType.ALL)).isFalse();
	}

	@Test
	void canWrite() {
		MediaType mediaType = new MediaType("foo", "bar");
		HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

		assertThat(converter.canWrite(MyType.class, mediaType)).isTrue();
		assertThat(converter.canWrite(MyType.class, new MediaType("foo", "*"))).isTrue();
		assertThat(converter.canWrite(MyType.class, MediaType.ALL)).isTrue();
	}

	@Test
	void canWriteWithWildcardInSupportedSubtype() {
		MediaType mediaType = new MediaType("foo");
		HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

		assertThat(converter.canWrite(MyType.class, new MediaType("foo", "bar"))).isTrue();
		assertThat(converter.canWrite(MyType.class, new MediaType("foo", "*"))).isTrue();
		assertThat(converter.canWrite(MyType.class, MediaType.ALL)).isTrue();
	}


	private static class MyHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

		private MyHttpMessageConverter(MediaType supportedMediaType) {
			super(supportedMediaType);
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return MyType.class.equals(clazz);
		}

		@Override
		protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
				throws HttpMessageNotReadableException {
			throw new AssertionError("Not expected");
		}

		@Override
		protected void writeInternal(T t, HttpOutputMessage outputMessage)
				throws HttpMessageNotWritableException {
			throw new AssertionError("Not expected");
		}
	}

	private static class MyType {

	}

}
