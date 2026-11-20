/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.core.serializer.support.SerializingConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link SerializingConverter} and {@link DeserializingConverter}.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @since 3.0.5
 */
class SerializationConverterTests {

	@Test
	void serializeAndDeserializeStringWithDefaultSerializer() {
		SerializingConverter toBytes = new SerializingConverter();
		byte[] bytes = toBytes.convert("Testing");
		DeserializingConverter fromBytes = new DeserializingConverter();
		assertThat(fromBytes.convert(bytes)).isEqualTo("Testing");
	}

	@Test
	void serializeAndDeserializeStringWithExplicitSerializer() {
		SerializingConverter toBytes = new SerializingConverter(new DefaultSerializer());
		byte[] bytes = toBytes.convert("Testing");
		DeserializingConverter fromBytes = new DeserializingConverter();
		assertThat(fromBytes.convert(bytes)).isEqualTo("Testing");
	}

	@Test
	void nonSerializableObject() {
		SerializingConverter toBytes = new SerializingConverter();
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> toBytes.convert(new Object()))
				.havingCause()
					.isInstanceOf(IllegalArgumentException.class)
					.withMessageContaining("requires a Serializable payload");
	}

	@Test
	void nonSerializableField() {
		SerializingConverter toBytes = new SerializingConverter();
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> toBytes.convert(new UnSerializable()))
				.withCauseInstanceOf(NotSerializableException.class);
	}

	@Test
	void deserializationFailure() {
		DeserializingConverter fromBytes = new DeserializingConverter();
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> fromBytes.convert("Junk".getBytes()));
	}

	@Test
	void deserializationWithExplicitClassLoader() {
		DeserializingConverter fromBytes = new DeserializingConverter(getClass().getClassLoader());
		SerializingConverter toBytes = new SerializingConverter();
		String expected = "SPRING FRAMEWORK";
		assertThat(fromBytes.convert(toBytes.convert(expected))).isEqualTo(expected);
	}

	@Test
	void deserializationWithExplicitDeserializer() {
		DeserializingConverter fromBytes = new DeserializingConverter(new DefaultDeserializer());
		SerializingConverter toBytes = new SerializingConverter();
		String expected = "SPRING FRAMEWORK";
		assertThat(fromBytes.convert(toBytes.convert(expected))).isEqualTo(expected);
	}

	@Test
	void deserializationIOException() {
		ClassNotFoundException classNotFoundException = new ClassNotFoundException();
		try (MockedConstruction<ConfigurableObjectInputStream> mocked =
				Mockito.mockConstruction(ConfigurableObjectInputStream.class,
					(mock, context) -> given(mock.readObject()).willThrow(classNotFoundException))) {
			DefaultDeserializer defaultSerializer = new DefaultDeserializer(getClass().getClassLoader());
			assertThat(mocked).isNotNull();
			assertThatIOException()
					.isThrownBy(() -> defaultSerializer.deserialize(new ByteArrayInputStream("test".getBytes())))
					.withMessage("Failed to deserialize object type")
					.havingCause().isSameAs(classNotFoundException);
		}
	}


	static class UnSerializable implements Serializable {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings({"unused", "serial"})
		private Object object = new Object();
	}

}
