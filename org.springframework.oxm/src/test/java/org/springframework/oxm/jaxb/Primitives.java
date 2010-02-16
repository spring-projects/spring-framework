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

package org.springframework.oxm.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * Used by {@link org.springframework.oxm.jaxb.Jaxb2MarshallerTests}.
 *
 * @author Arjen Poutsma
 */
public class Primitives {

	private static final QName NAME = new QName("http://springframework.org/oxm-test", "primitives");

	// following methods are used to test support for primitives
	public JAXBElement<Boolean> primitiveBoolean() {
		return new JAXBElement<Boolean>(NAME, Boolean.class, true);
	}

	public JAXBElement<Byte> primitiveByte() {
		return new JAXBElement<Byte>(NAME, Byte.class, (byte)42);
	}

	public JAXBElement<Short> primitiveShort() {
		return new JAXBElement<Short>(NAME, Short.class, (short)42);
	}

	public JAXBElement<Integer> primitiveInteger() {
		return new JAXBElement<Integer>(NAME, Integer.class, 42);
	}

	public JAXBElement<Long> primitiveLong() {
		return new JAXBElement<Long>(NAME, Long.class, 42L);
	}

	public JAXBElement<Double> primitiveDouble() {
		return new JAXBElement<Double>(NAME, Double.class, 42D);
	}
	
	public JAXBElement<byte[]> primitiveByteArray() {
		return new JAXBElement<byte[]>(NAME, byte[].class, new byte[]{42});
	}


}
