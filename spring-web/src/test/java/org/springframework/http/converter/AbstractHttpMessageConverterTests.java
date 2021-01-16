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

package org.springframework.http.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpOutputMessage;

import java.io.IOException;
import java.util.Optional;

/**
 * Test cases for {@link AbstractHttpMessageConverter} class.
 *
 * @author Vladislav Kisel
 */
public class AbstractHttpMessageConverterTests {

	@Test
	public void testValueUnboxingWhenTypeIsObject() throws IOException {
		TestConverterWithObjectType converter = new TestConverterWithObjectType();
		converter.write(Optional.of("123"), MediaType.ALL, new MockHttpOutputMessage());

		Assertions.assertEquals("123", converter.t);
	}

	@Test
	public void testValueUnboxingWhenValueIsNull() throws IOException {
		TestConverterWithObjectType converter = new TestConverterWithObjectType();
		converter.write(Optional.empty(), MediaType.ALL, new MockHttpOutputMessage());

		Assertions.assertEquals(Optional.empty(), converter.t);
	}

	@Test
	public void testValueUnboxingWhenTypeIsOptional() throws IOException {
		TestConverterWithOptionalType converter = new TestConverterWithOptionalType();
		converter.write(Optional.of("123"), MediaType.ALL, new MockHttpOutputMessage());

		Assertions.assertEquals(Optional.of("123"), converter.t);
	}

	@Test
	public void testValueUnboxingWhenTypeIsUnknown() throws IOException {
		GenericTestConverter<Optional<String>> converter = new GenericTestConverter<>();
		converter.write(Optional.of("123"), MediaType.ALL, new MockHttpOutputMessage());

		Assertions.assertEquals(Optional.of("123"), converter.t);
	}

	private static class GenericTestConverter<T> extends AbstractHttpMessageConverter<T> {

		T t;

		@Override
		protected boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
			return t;
		}

		@Override
		protected void writeInternal(T t, HttpOutputMessage outputMessage) throws HttpMessageNotWritableException {
			this.t = t;
		}
	}

	private static class TestConverterWithOptionalType extends GenericTestConverter<Optional<String>> {
	}

	private static class TestConverterWithObjectType extends GenericTestConverter<Object> {
	}

}
