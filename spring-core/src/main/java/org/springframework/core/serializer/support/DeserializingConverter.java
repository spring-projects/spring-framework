/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.serializer.support;

import java.io.ByteArrayInputStream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.util.Assert;

/**
 * A {@link Converter} that delegates to a
 * {@link org.springframework.core.serializer.Deserializer}
 * to convert data in a byte array to an object.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 */
public class DeserializingConverter implements Converter<byte[], Object> {

	private final Deserializer<Object> deserializer;


	/**
	 * Create a {@code DeserializingConverter} with default {@link java.io.ObjectInputStream}
	 * configuration, using the "latest user-defined ClassLoader".
	 * @see DefaultDeserializer#DefaultDeserializer()
	 */
	public DeserializingConverter() {
		this.deserializer = new DefaultDeserializer();
	}

	/**
	 * Create a {@code DeserializingConverter} for using an {@link java.io.ObjectInputStream}
	 * with the given {@code ClassLoader}.
	 * @param classLoader the ClassLoader to use
	 * @since 4.2.1
	 * @see DefaultDeserializer#DefaultDeserializer(ClassLoader)
	 */
	public DeserializingConverter(@Nullable ClassLoader classLoader) {
		this.deserializer = new DefaultDeserializer(classLoader);
	}

	/**
	 * Create a {@code DeserializingConverter} that delegates to the provided {@link Deserializer}.
	 */
	public DeserializingConverter(Deserializer<Object> deserializer) {
		Assert.notNull(deserializer, "Deserializer must not be null");
		this.deserializer = deserializer;
	}


	@Override
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
