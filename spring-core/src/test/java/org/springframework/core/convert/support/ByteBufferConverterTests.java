/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ByteBufferConverter}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
class ByteBufferConverterTests {

	private final GenericConversionService conversionService = new DefaultConversionService();


	@BeforeEach
	void setup() {
		conversionService.addConverter(new ByteArrayToOtherTypeConverter());
		conversionService.addConverter(new OtherTypeToByteArrayConverter());
	}


	@Test
	void byteArrayToByteBuffer() {
		byte[] bytes = new byte[] { 1, 2, 3 };
		ByteBuffer convert = conversionService.convert(bytes, ByteBuffer.class);
		assertThat(convert.array()).isNotSameAs(bytes);
		assertThat(convert.array()).isEqualTo(bytes);
	}

	@Test
	void byteBufferToByteArray() {
		byte[] bytes = new byte[] { 1, 2, 3 };
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		byte[] convert = conversionService.convert(byteBuffer, byte[].class);
		assertThat(convert).isNotSameAs(bytes);
		assertThat(convert).isEqualTo(bytes);
	}

	@Test
	void byteBufferToOtherType() {
		byte[] bytes = new byte[] { 1, 2, 3 };
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		OtherType convert = conversionService.convert(byteBuffer, OtherType.class);
		assertThat(convert.bytes).isNotSameAs(bytes);
		assertThat(convert.bytes).isEqualTo(bytes);
	}

	@Test
	void otherTypeToByteBuffer() {
		byte[] bytes = new byte[] { 1, 2, 3 };
		OtherType otherType = new OtherType(bytes);
		ByteBuffer convert = conversionService.convert(otherType, ByteBuffer.class);
		assertThat(convert.array()).isNotSameAs(bytes);
		assertThat(convert.array()).isEqualTo(bytes);
	}

	@Test
	void byteBufferToByteBuffer() {
		byte[] bytes = new byte[] { 1, 2, 3 };
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		ByteBuffer convert = conversionService.convert(byteBuffer, ByteBuffer.class);
		assertThat(convert).isNotSameAs(byteBuffer.rewind());
		assertThat(convert).isEqualTo(byteBuffer.rewind());
		assertThat(convert).isEqualTo(ByteBuffer.wrap(bytes));
		assertThat(convert.array()).isEqualTo(bytes);
	}


	private static class OtherType {

		private byte[] bytes;

		public OtherType(byte[] bytes) {
			this.bytes = bytes;
		}

	}

	private static class ByteArrayToOtherTypeConverter implements Converter<byte[], OtherType> {

		@Override
		public OtherType convert(byte[] source) {
			return new OtherType(source);
		}
	}


	private static class OtherTypeToByteArrayConverter implements Converter<OtherType, byte[]> {

		@Override
		public byte[] convert(OtherType source) {
			return source.bytes;
		}

	}

}
