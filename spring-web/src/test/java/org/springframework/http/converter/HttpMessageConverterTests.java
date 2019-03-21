/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.IOException;

import org.junit.Test;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test-case for AbstractHttpMessageConverter.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class HttpMessageConverterTests {


	@Test
	public void canRead() {
		MediaType mediaType = new MediaType("foo", "bar");
		HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<MyType>(mediaType);

		assertTrue(converter.canRead(MyType.class, mediaType));
		assertFalse(converter.canRead(MyType.class, new MediaType("foo", "*")));
		assertFalse(converter.canRead(MyType.class, MediaType.ALL));
	}

	@Test
	public void canReadWithWildcardSubtype() {
		MediaType mediaType = new MediaType("foo");
		HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<MyType>(mediaType);

		assertTrue(converter.canRead(MyType.class, new MediaType("foo", "bar")));
		assertTrue(converter.canRead(MyType.class, new MediaType("foo", "*")));
		assertFalse(converter.canRead(MyType.class, MediaType.ALL));
	}

	@Test
	public void canWrite() {
		MediaType mediaType = new MediaType("foo", "bar");
		HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<MyType>(mediaType);

		assertTrue(converter.canWrite(MyType.class, mediaType));
		assertTrue(converter.canWrite(MyType.class, new MediaType("foo", "*")));
		assertTrue(converter.canWrite(MyType.class, MediaType.ALL));
	}

	@Test
	public void canWriteWithWildcardInSupportedSubtype() {
		MediaType mediaType = new MediaType("foo");
		HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<MyType>(mediaType);

		assertTrue(converter.canWrite(MyType.class, new MediaType("foo", "bar")));
		assertTrue(converter.canWrite(MyType.class, new MediaType("foo", "*")));
		assertTrue(converter.canWrite(MyType.class, MediaType.ALL));
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
				throws IOException, HttpMessageNotReadableException {
			fail("Not expected");
			return null;
		}

		@Override
		protected void writeInternal(T t, HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {
			fail("Not expected");
		}
	}

	private static class MyType {

	}

}
