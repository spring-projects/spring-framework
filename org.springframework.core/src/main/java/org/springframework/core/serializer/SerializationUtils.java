/*
 * Copyright 2002-2010 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Static utility for serialization and deserialization.
 * 
 * @author Dave Syer
 * @since 3.0.5
 */
public abstract class SerializationUtils {

	/**
	 * Serialize the object provided.
	 * 
	 * @param object the object to serialize
	 * @return an array of bytes representing the object in a portable fashion
	 */
	public static byte[] serialize(Object object) {
		if (object == null) {
			return null;
		}
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(stream).writeObject(object);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("failed to serialize object of type: " + object.getClass(), e);
		}
		return stream.toByteArray();
	}

	/**
	 * Deserialize the byte array into an object.
	 * 
	 * @param bytes a serialized object
	 * @return the result of deserializing the bytes
	 */
	public static Object deserialize(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		try {
			return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
		}
		catch (IOException e) {
			throw new IllegalArgumentException("failed to deserialize object", e);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("failed to deserialize object type", e);
		}
	}

}
