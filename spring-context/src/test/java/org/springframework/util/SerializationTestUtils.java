/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Utilities for testing serializability of objects.
 * Exposes static methods for use in other test cases.
 *
 * @author Rod Johnson
 */
public class SerializationTestUtils {
	
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

}
