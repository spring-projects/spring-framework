/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import org.springframework.core.serializer.support.SerializationDelegate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests for {@link Serializer}, {@link Deserializer}, and {@link SerializationDelegate}.
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
