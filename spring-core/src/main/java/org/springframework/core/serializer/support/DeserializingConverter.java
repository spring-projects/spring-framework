/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.serializer.support;

import java.io.ByteArrayInputStream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.util.Assert;

/**
 * A {@link Converter} that delegates to a {@link org.springframework.core.serializer.Deserializer}
 * to convert data in a byte array to an object.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @since 3.0.5
 */
public class DeserializingConverter implements Converter<byte[], Object> {

	private final Deserializer<Object> deserializer;


	/**
	 * Create a default DeserializingConverter that uses standard Java deserialization.
	 */
	public DeserializingConverter() {
		this.deserializer = new DefaultDeserializer();
	}

	/**
	 * Create a DeserializingConverter that delegates to the provided {@link Deserializer}.
	 */
	public DeserializingConverter(Deserializer<Object> deserializer) {
		Assert.notNull(deserializer, "Deserializer must not be null");
		this.deserializer = deserializer;
	}


	public Object convert(byte[] source) {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(source);
		try {
			return this.deserializer.deserialize(byteStream);
		}
		catch (Throwable ex) {
			throw new SerializationFailedException("Failed to deserialize payload. " +
					"Is the byte array a result of corresponding serialization for " +
					this.deserializer.getClass().getSimpleName() + "?", ex);
		}
	}

}
