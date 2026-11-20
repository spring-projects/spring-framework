/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Utilities for testing serializability of objects.
 *
 * <p>Exposes static methods for use in other test cases.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 */
public class SerializationTestUtils {

	public static void testSerialization(Object o) throws IOException {
		OutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(o);
		}
	}

	public static boolean isSerializable(Object o) throws IOException {
		try {
			testSerialization(o);
			return true;
		}
		catch (NotSerializableException ex) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T serializeAndDeserialize(T o) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(o);
			oos.flush();
		}
		byte[] bytes = baos.toByteArray();

		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			return (T) ois.readObject();
		}
	}

	public static <T> T serializeAndDeserialize(Object o, Class<T> expectedType) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(o);
			oos.flush();
		}
		byte[] bytes = baos.toByteArray();

		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			return expectedType.cast(ois.readObject());
		}
	}

}
