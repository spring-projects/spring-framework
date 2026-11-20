/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A strategy interface for streaming an object to an OutputStream.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 * @param <T> the object type
 * @see Deserializer
 */
@FunctionalInterface
public interface Serializer<T> {

	/**
	 * Write an object of type T to the given OutputStream.
	 * <p>Note: Implementations should not close the given OutputStream
	 * (or any decorators of that OutputStream) but rather leave this up
	 * to the caller.
	 * @param object the object to serialize
	 * @param outputStream the output stream
	 * @throws IOException in case of errors writing to the stream
	 */
	void serialize(T object, OutputStream outputStream) throws IOException;

	/**
	 * Turn an object of type T into a serialized byte array.
	 * @param object the object to serialize
	 * @return the resulting byte array
	 * @throws IOException in case of serialization failure
	 * @since 5.2.7
	 */
	default byte[] serializeToByteArray(T object) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		serialize(object, out);
		return out.toByteArray();
	}

}
