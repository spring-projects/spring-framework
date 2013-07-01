/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link ByteBufferConverter}.
 *
 * @author Phillip Webb
 */
public class ByteBufferConverterTests {

	private GenericConversionService conversionService;

	@Before
	public void setup() {
		this.conversionService = new GenericConversionService();
		this.conversionService.addConverter(new ByteBufferConverter(conversionService));
		this.conversionService.addConverter(new ByteArrayToOtherTypeConverter());
		this.conversionService.addConverter(new OtherTypeToByteArrayConverter());
	}

	@Test
	public void byteArrayToByteBuffer() throws Exception {
		byte[] bytes = new byte[] { 1, 2, 3 };
		ByteBuffer convert = this.conversionService.convert(bytes, ByteBuffer.class);
		assertThat(bytes, not(sameInstance(convert.array())));
		assertThat(bytes, equalTo(convert.array()));
	}

	@Test
	public void byteBufferToByteArray() throws Exception {
		byte[] bytes = new byte[] { 1, 2, 3 };
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		byte[] convert = this.conversionService.convert(byteBuffer, byte[].class);
		assertThat(bytes, not(sameInstance(convert)));
		assertThat(bytes, equalTo(convert));
	}

	@Test
	public void byteBufferToOtherType() throws Exception {
		byte[] bytes = new byte[] { 1, 2, 3 };
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		OtherType convert = this.conversionService.convert(byteBuffer, OtherType.class);
		assertThat(bytes, not(sameInstance(convert.bytes)));
		assertThat(bytes, equalTo(convert.bytes));
	}

	@Test
	public void otherTypeToByteBuffer() throws Exception {
		byte[] bytes = new byte[] { 1, 2, 3 };
		OtherType otherType = new OtherType(bytes);
		ByteBuffer convert = this.conversionService.convert(otherType, ByteBuffer.class);
		assertThat(bytes, not(sameInstance(convert.array())));
		assertThat(bytes, equalTo(convert.array()));
	}

	private static class OtherType {

		private byte[] bytes;

		public OtherType(byte[] bytes) {
			this.bytes = bytes;
		}

	}

	private static class ByteArrayToOtherTypeConverter implements
			Converter<byte[], OtherType> {

		@Override
		public OtherType convert(byte[] source) {
			return new OtherType(source);
		}
	}

	private static class OtherTypeToByteArrayConverter implements
			Converter<OtherType, byte[]> {

		@Override
		public byte[] convert(OtherType source) {
			return source.bytes;
		}

	}

}
