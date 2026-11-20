/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.serializer.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.util.Assert;

/**
 * A {@link Converter} that delegates to a
 * {@link org.springframework.core.serializer.Serializer}
 * to convert an object to a byte array.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @since 3.0.5
 */
public class SerializingConverter implements Converter<Object, byte[]> {

	private final Serializer<Object> serializer;


	/**
	 * Create a default {@code SerializingConverter} that uses standard Java serialization.
	 */
	public SerializingConverter() {
		this.serializer = new DefaultSerializer();
	}

	/**
	 * Create a {@code SerializingConverter} that delegates to the provided {@link Serializer}.
	 */
	public SerializingConverter(Serializer<Object> serializer) {
		Assert.notNull(serializer, "Serializer must not be null");
		this.serializer = serializer;
	}


	/**
	 * Serializes the source object and returns the byte array result.
	 */
	@Override
	public byte[] convert(Object source) {
		try {
			return this.serializer.serializeToByteArray(source);
		}
		catch (Throwable ex) {
			throw new SerializationFailedException("Failed to serialize object using " +
					this.serializer.getClass().getSimpleName(), ex);
		}
	}

}
