/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.NotSerializableException;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.core.serializer.support.SerializingConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gary Russell
 * @author Mark Fisher
 * @since 3.0.5
 */
class SerializationConverterTests {

	@Test
	void serializeAndDeserializeString() {
		SerializingConverter toBytes = new SerializingConverter();
		byte[] bytes = toBytes.convert("Testing");
		DeserializingConverter fromBytes = new DeserializingConverter();
		assertThat(fromBytes.convert(bytes)).isEqualTo("Testing");
	}

	@Test
	void nonSerializableObject() {
		SerializingConverter toBytes = new SerializingConverter();
		assertThatExceptionOfType(SerializationFailedException.class).isThrownBy(() ->
				toBytes.convert(new Object()))
			.withCauseInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void nonSerializableField() {
		SerializingConverter toBytes = new SerializingConverter();
		assertThatExceptionOfType(SerializationFailedException.class).isThrownBy(() ->
				toBytes.convert(new UnSerializable()))
			.withCauseInstanceOf(NotSerializableException.class);
	}

	@Test
	void deserializationFailure() {
		DeserializingConverter fromBytes = new DeserializingConverter();
		assertThatExceptionOfType(SerializationFailedException.class).isThrownBy(() ->
				fromBytes.convert("Junk".getBytes()));
	}


	class UnSerializable implements Serializable {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		private Object object;
	}

}
