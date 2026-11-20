/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.serializer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * A {@link Serializer} implementation that writes an object to an output stream
 * using Java serialization.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @since 3.0.5
 */
public class DefaultSerializer implements Serializer<Object> {

	/**
	 * Writes the source object to an output stream using Java serialization.
	 * The source object must implement {@link Serializable}.
	 * @see ObjectOutputStream#writeObject(Object)
	 */
	@Override
	public void serialize(Object object, OutputStream outputStream) throws IOException {
		if (!(object instanceof Serializable)) {
			throw new IllegalArgumentException(getClass().getSimpleName() + " requires a Serializable payload " +
					"but received an object of type [" + object.getClass().getName() + "]");
		}
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		objectOutputStream.writeObject(object);
		objectOutputStream.flush();
	}

}
