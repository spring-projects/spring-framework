/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ConfigurableObjectInputStream;

/**
 * A default {@link Deserializer} implementation that reads an input stream
 * using Java serialization.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 * @see ObjectInputStream
 */
public class DefaultDeserializer implements Deserializer<Object> {

	private final @Nullable ClassLoader classLoader;

	private final @Nullable ObjectInputFilter objectInputFilter;


	/**
	 * Create a {@code DefaultDeserializer} with default {@link ObjectInputStream}
	 * configuration, using the "latest user-defined ClassLoader".
	 */
	public DefaultDeserializer() {
		this.classLoader = null;
		this.objectInputFilter = null;
	}

	/**
	 * Create a {@code DefaultDeserializer} for using an {@link ObjectInputStream}
	 * with the given {@code ClassLoader}.
	 * @param classLoader the ClassLoader to use
	 * @since 4.2.1
	 * @see ConfigurableObjectInputStream#ConfigurableObjectInputStream(InputStream, ClassLoader)
	 */
	public DefaultDeserializer(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
		this.objectInputFilter = null;
	}

	/**
	 * Create a {@code DefaultDeserializer} for using an {@link ObjectInputStream}
	 * with the given {@code ClassLoader}.
	 * @param classLoader the ClassLoader to use
	 * @param objectInputFilter a custom ObjectInputFilter to apply
	 * @since 7.0.9
	 * @see ConfigurableObjectInputStream#ConfigurableObjectInputStream(InputStream, ClassLoader)
	 * @see ObjectInputStream#setObjectInputFilter
	 */
	public DefaultDeserializer(@Nullable ClassLoader classLoader, @Nullable ObjectInputFilter objectInputFilter) {
		this.classLoader = classLoader;
		this.objectInputFilter = objectInputFilter;
	}


	/**
	 * Return the {@link ClassLoader} to use for deserialization, or {@code null}
	 * to use the "latest user-defined ClassLoader".
	 * @since 6.2.19
	 * @see ConfigurableObjectInputStream#ConfigurableObjectInputStream(InputStream, ClassLoader)
	 */
	public final @Nullable ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Return the {@link ObjectInputFilter} to apply to the {@link ObjectInputStream},
	 * if any.
	 * @since 7.0.9
	 * @see ObjectInputStream#setObjectInputFilter
	 */
	public final @Nullable ObjectInputFilter getObjectInputFilter() {
		return this.objectInputFilter;
	}


	/**
	 * Read from the supplied {@code InputStream} and deserialize the contents
	 * into an object.
	 * @see ObjectInputStream#readObject()
	 */
	@Override
	public Object deserialize(InputStream inputStream) throws IOException {
		ObjectInputStream objectInputStream = new ConfigurableObjectInputStream(inputStream, this.classLoader);
		if (this.objectInputFilter != null) {
			objectInputStream.setObjectInputFilter(this.objectInputFilter);
		}
		try {
			return objectInputStream.readObject();
		}
		catch (ClassNotFoundException ex) {
			throw new IOException("Failed to deserialize object type", ex);
		}
	}

}
