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


class SerializerTests {

	private static final String SPRING_FRAMEWORK = "Spring Framework";


	@Test
	void serializeToByteArray() throws IOException {
		SpyStringSerializer serializer = new SpyStringSerializer<String>();
		serializer.serializeToByteArray(SPRING_FRAMEWORK);
		assertThat(serializer.expectedObject).isEqualTo(SPRING_FRAMEWORK);
		assertThat(serializer.expectedOs).isNotNull();
	}

	@Test
	void deserializeToByteArray() throws IOException {
		SpyStringDeserializer deserializer = new SpyStringDeserializer();
		deserializer.deserializeFromByteArray(SPRING_FRAMEWORK.getBytes());
		assertThat(deserializer.expectedObject).isEqualTo(SPRING_FRAMEWORK);
	}

	@Test
	void serializationDelegate() throws IOException {
		SerializationDelegate delegate = new SerializationDelegate(new DefaultSerializer(), new DefaultDeserializer());
		byte[] serializedObj = delegate.serializeToByteArray(SPRING_FRAMEWORK);
		Object deserializedObj = delegate.deserialize(new ByteArrayInputStream(serializedObj));
		assertThat(deserializedObj).isEqualTo(SPRING_FRAMEWORK);
	}

	@Test
	void serializationDelegateWithClassLoader() throws IOException {
		SerializationDelegate delegate = new SerializationDelegate(this.getClass().getClassLoader());
		byte[] serializedObj = delegate.serializeToByteArray(SPRING_FRAMEWORK);
		Object deserializedObj = delegate.deserialize(new ByteArrayInputStream(serializedObj));
		assertThat(deserializedObj).isEqualTo(SPRING_FRAMEWORK);
	}

	static class SpyStringSerializer<T> implements Serializer<T> {
		T expectedObject;
		OutputStream expectedOs;

		@Override
		public void serialize(T object, OutputStream outputStream) {
			expectedObject = object;
			expectedOs = outputStream;
		}
	}

	static class SpyStringDeserializer implements Deserializer<Object> {
		Object expectedObject;


		@Override
		public String deserialize(InputStream inputStream) {
			expectedObject = SPRING_FRAMEWORK;
			return SPRING_FRAMEWORK;
		}
	}
}
