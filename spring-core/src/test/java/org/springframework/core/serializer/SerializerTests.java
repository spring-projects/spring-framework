/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import org.springframework.core.serializer.support.SerializationDelegate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Serializer}, {@link Deserializer}, and {@link SerializationDelegate}.
 *
 * @since 6.1
 */
class SerializerTests {

	private static final String SPRING_FRAMEWORK = "Spring Framework";


	@Test
	void serializeToByteArray() throws IOException {

		class SpyStringSerializer implements Serializer<String> {

			String expectedObject;
			OutputStream expectedOutputStream;

			@Override
			public void serialize(String object, OutputStream outputStream) {
				this.expectedObject = object;
				this.expectedOutputStream = outputStream;
			}
		}

		SpyStringSerializer serializer = new SpyStringSerializer();
		serializer.serializeToByteArray(SPRING_FRAMEWORK);
		assertThat(serializer.expectedObject).isEqualTo(SPRING_FRAMEWORK);
		assertThat(serializer.expectedOutputStream).isNotNull();
	}

	@Test
	void deserializeToByteArray() throws IOException {

		class SpyStringDeserializer implements Deserializer<String> {

			InputStream expectedInputStream;

			@Override
			public String deserialize(InputStream inputStream) {
				this.expectedInputStream = inputStream;
				return SPRING_FRAMEWORK;
			}
		}

		SpyStringDeserializer deserializer = new SpyStringDeserializer();
		Object deserializedObj = deserializer.deserializeFromByteArray(SPRING_FRAMEWORK.getBytes());
		assertThat(deserializedObj).isEqualTo(SPRING_FRAMEWORK);
		assertThat(deserializer.expectedInputStream).isNotNull();
	}

	@Test  // gh-36833
	void defaultDeserializerExposesNullClassLoaderByDefault() {
		assertThat(new DefaultDeserializer().getClassLoader()).isNull();
	}

	@Test  // gh-36833
	void defaultDeserializerExposesConfiguredClassLoader() {
		ClassLoader classLoader = getClass().getClassLoader();
		assertThat(new DefaultDeserializer(classLoader).getClassLoader()).isSameAs(classLoader);
	}

	@Test
	void serializationDelegateWithExplicitSerializerAndDeserializer() throws IOException {
		SerializationDelegate delegate = new SerializationDelegate(new DefaultSerializer(), new DefaultDeserializer());
		byte[] serializedObj = delegate.serializeToByteArray(SPRING_FRAMEWORK);
		Object deserializedObj = delegate.deserialize(new ByteArrayInputStream(serializedObj));
		assertThat(deserializedObj).isEqualTo(SPRING_FRAMEWORK);
	}

	@Test
	void serializationDelegateWithExplicitClassLoader() throws IOException {
		SerializationDelegate delegate = new SerializationDelegate(getClass().getClassLoader());
		byte[] serializedObj = delegate.serializeToByteArray(SPRING_FRAMEWORK);
		Object deserializedObj = delegate.deserialize(new ByteArrayInputStream(serializedObj));
		assertThat(deserializedObj).isEqualTo(SPRING_FRAMEWORK);
	}

}
