/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 * @author JongMin Moon
 * @since 4.1
 */
public class PropertyValueTests implements Serializable {

	boolean serializable;

	@Test
	public void testSerializableWithPropertyDescriptor() throws Exception {
		PropertyValue value = new PropertyValue("a", "b");
		value.resolvedDescriptor = new PropertyDescriptor("serializable",
				PropertyValueTests.class);

		PropertyValue value2 = serializeAndDeserialize(value);
		assertEquals(value.getName(), value2.getName());
		assertEquals(value.getValue(), value2.getValue());
	}

	@Test
	public void testSerializableWithSubPropertyDescriptor() throws Exception {
		PropertyValue value = new PropertyValue("a", "b");
		value.resolvedDescriptor = new SubPropertyDescriptor("serializable",
				PropertyValueTests.class);

		PropertyValue value2 = serializeAndDeserialize(value);
		assertEquals(value.getName(), value2.getName());
		assertEquals(value.getValue(), value2.getValue());
	}

	private PropertyValue serializeAndDeserialize(PropertyValue value) throws Exception {
		String filename = "PropertyValue.ser";
		new File(filename).delete(); // before test, just try to delete.

		// save the object to file
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(filename);
			out = new ObjectOutputStream(fos);
			out.writeObject(value);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			new File(filename).delete();
			throw ex;
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (Exception ignore) {
				}
			}
		}
		// read the object from file
		FileInputStream fis = null;
		ObjectInputStream in = null;
		PropertyValue value2;
		try {
			fis = new FileInputStream(filename);
			in = new ObjectInputStream(fis);
			value2 = (PropertyValue) in.readObject();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally {
			new File(filename).delete();
			if (in != null) {
				try {
					in.close();
				}
				catch (Exception ignore) {
				}
			}
		}
		return value2;
	}

	public boolean isSerializable() {
		return true;
	}

	public void setSerializable(boolean serializable) {
		this.serializable = serializable;
	}

	public class SubPropertyDescriptor extends PropertyDescriptor implements Serializable {

		public SubPropertyDescriptor(String propertyName, Class<?> beanClass)
				throws IntrospectionException {
			super(propertyName, beanClass);
		}

		public SubPropertyDescriptor() throws IntrospectionException {
			// There is no-args constructor in the PropertyDescriptor.
			super("serializable", PropertyValueTests.class);
		}

	}

}
