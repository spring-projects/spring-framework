/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm.jaxb;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;

/**
 * Used by {@link org.springframework.oxm.jaxb.Jaxb2MarshallerTests}.
 *
 * @author Arjen Poutsma
 */
public class Primitives {

	private static final QName NAME = new QName("https://springframework.org/oxm-test", "primitives");

	// following methods are used to test support for primitives
	public JAXBElement<Boolean> primitiveBoolean() {
		return new JAXBElement<>(NAME, Boolean.class, true);
	}

	public JAXBElement<Byte> primitiveByte() {
		return new JAXBElement<>(NAME, Byte.class, (byte) 42);
	}

	public JAXBElement<Short> primitiveShort() {
		return new JAXBElement<>(NAME, Short.class, (short) 42);
	}

	public JAXBElement<Integer> primitiveInteger() {
		return new JAXBElement<>(NAME, Integer.class, 42);
	}

	public JAXBElement<Long> primitiveLong() {
		return new JAXBElement<>(NAME, Long.class, 42L);
	}

	public JAXBElement<Double> primitiveDouble() {
		return new JAXBElement<>(NAME, Double.class, 42D);
	}

	public JAXBElement<byte[]> primitiveByteArray() {
		return new JAXBElement<>(NAME, byte[].class, new byte[] {42});
	}


}
