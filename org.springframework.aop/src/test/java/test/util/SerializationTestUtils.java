/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package test.util;

import static org.junit.Assert.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.junit.Test;

import test.beans.TestBean;

/**
 * Utilities for testing serializability of objects.
 * Exposes static methods for use in other test cases.
 * Contains {@link org.junit.Test} methods to test itself.
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class SerializationTestUtils {
	
	public static void testSerialization(Object o) throws IOException {
		OutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
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
	
	public static Object serializeAndDeserialize(Object o) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.flush();
		baos.flush();
		byte[] bytes = baos.toByteArray();
		
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(is);
		Object o2 = ois.readObject();
		
		return o2;
	}
	
	@Test(expected=NotSerializableException.class)
	public void testWithNonSerializableObject() throws IOException {
		TestBean o = new TestBean();
		assertFalse(o instanceof Serializable);
		assertFalse(isSerializable(o));
		
		testSerialization(o);
	}
	
	@Test
	public void testWithSerializableObject() throws Exception {
		int x = 5;
		int y = 10;
		Point p = new Point(x, y);
		assertTrue(p instanceof Serializable);
	
		testSerialization(p);
		
		assertTrue(isSerializable(p));
		
		Point p2 = (Point) serializeAndDeserialize(p);
		assertNotSame(p, p2);
		assertEquals(x, (int) p2.getX());
		assertEquals(y, (int) p2.getY());
	}

}
