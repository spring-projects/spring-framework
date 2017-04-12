/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;

import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.core.serializer.support.SerializingConverter;

import static org.junit.Assert.*;

/**
 * @author Gary Russell
 * @author Mark Fisher
 * @since 3.0.5
 */
public class SerializationConverterTests {

	@Test
	public void serializeAndDeserializeString() {
		SerializingConverter toBytes = new SerializingConverter();
		byte[] bytes = toBytes.convert("Testing");
		DeserializingConverter fromBytes = new DeserializingConverter();
		assertEquals("Testing", fromBytes.convert(bytes));
	}

	@Test
	public void nonSerializableObject() {
		SerializingConverter toBytes = new SerializingConverter();
		try {
			toBytes.convert(new Object());
			fail("Expected IllegalArgumentException");
		}
		catch (SerializationFailedException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
	}

	@Test
	public void nonSerializableField() {
		SerializingConverter toBytes = new SerializingConverter();
		try {
			toBytes.convert(new UnSerializable());
			fail("Expected SerializationFailureException");
		}
		catch (SerializationFailedException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof NotSerializableException);
		}
	}

	@Test(expected = SerializationFailedException.class)
	public void deserializationFailure() {
		DeserializingConverter fromBytes = new DeserializingConverter();
		fromBytes.convert("Junk".getBytes());
	}


	class UnSerializable implements Serializable {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		private Object object;
	}

}
