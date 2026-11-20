/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;

/**
 * Static utilities for serialization and deserialization using
 * <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/serialization/"
 * target="_blank">Java Object Serialization</a>.
 *
 * <p><strong>WARNING</strong>: These utilities should be used with caution. See
 * <a href="https://www.oracle.com/java/technologies/javase/seccodeguide.html#8"
 * target="_blank">Secure Coding Guidelines for the Java Programming Language</a>
 * for details.
 *
 * @author Dave Syer
 * @author Loïc Ledoyen
 * @author Sam Brannen
 * @since 3.0.5
 */
public abstract class SerializationUtils {

	/**
	 * Serialize the given object to a byte array.
	 * @param object the object to serialize
	 * @return an array of bytes representing the object in a portable fashion
	 */
	@Contract("null -> null")
	public static byte @Nullable [] serialize(@Nullable Object object) {
		if (object == null) {
			return null;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(object);
			oos.flush();
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to serialize object of type: " + object.getClass(), ex);
		}
		return baos.toByteArray();
	}

	/**
	 * Deserialize the byte array into an object.
	 * @param bytes a serialized object
	 * @return the result of deserializing the bytes
	 * @deprecated This utility uses Java Object Serialization, which allows
	 * arbitrary code to be run and is known for being the source of many Remote
	 * Code Execution (RCE) vulnerabilities.
	 * <p>Prefer the use of an external tool (that serializes to JSON, XML, or
	 * any other format) which is regularly checked and updated for not allowing RCE.
	 */
	@Deprecated(since = "6.0")
	@Contract("null -> null")
	public static @Nullable Object deserialize(byte @Nullable [] bytes) {
		if (bytes == null) {
			return null;
		}
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			return ois.readObject();
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to deserialize object", ex);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to deserialize object type", ex);
		}
	}

	/**
	 * Clone the given object using Java Object Serialization.
	 * @param object the object to clone
	 * @param <T> the type of the object to clone
	 * @return a clone (deep-copy) of the given object
	 * @since 6.0
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T clone(T object) {
		Object result = SerializationUtils.deserialize(SerializationUtils.serialize(object));
		Assert.state(result != null, "Deserialized object must not be null");
		return (T) result;
	}

}
