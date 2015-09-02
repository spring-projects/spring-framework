/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.core.NestedIOException;

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

	private final ClassLoader classLoader;


	/**
	 * Create a {@code DefaultDeserializer} with default {@link ObjectInputStream}
	 * configuration, using the "latest user-defined ClassLoader".
	 */
	public DefaultDeserializer() {
		this.classLoader = null;
	}

	/**
	 * Create a {@code DefaultDeserializer} for using an {@link ObjectInputStream}
	 * with the given {@code ClassLoader}.
	 * @since 4.2.1
	 * @see ConfigurableObjectInputStream#ConfigurableObjectInputStream(InputStream, ClassLoader)
	 */
	public DefaultDeserializer(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * Read from the supplied {@code InputStream} and deserialize the contents
	 * into an object.
	 * @see ObjectInputStream#readObject()
	 */
	@Override
	@SuppressWarnings("resource")
	public Object deserialize(InputStream inputStream) throws IOException {
		ObjectInputStream objectInputStream = new ConfigurableObjectInputStream(inputStream, this.classLoader);
		try {
			return objectInputStream.readObject();
		}
		catch (ClassNotFoundException ex) {
			throw new NestedIOException("Failed to deserialize object type", ex);
		}
	}

}
