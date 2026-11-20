/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

/**
 * Captures primitive types and their corresponding class objects, plus one special
 * {@link #OBJECT} entry that represents all reference (non-primitive) types.
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
public enum TypeCode {

	/**
	 * An {@link Object}.
	 */
	OBJECT(Object.class),

	/**
	 * A {@code boolean}.
	 */
	BOOLEAN(boolean.class),

	/**
	 * A {@code char}.
	 */
	CHAR(char.class),

	/**
	 * A {@code byte}.
	 */
	BYTE(byte.class),

	/**
	 * A {@code short}.
	 */
	SHORT(short.class),

	/**
	 * An {@code int}.
	 */
	INT(int.class),

	/**
	 * A {@code long}.
	 */
	LONG(long.class),

	/**
	 * A {@code float}.
	 */
	FLOAT(float.class),

	/**
	 * A {@code double}.
	 */
	DOUBLE(double.class);


	private final Class<?> type;


	TypeCode(Class<?> type) {
		this.type = type;
	}


	public Class<?> getType() {
		return this.type;
	}


	public static TypeCode forName(String name) {
		for (TypeCode typeCode : values()) {
			if (typeCode.name().equalsIgnoreCase(name)) {
				return typeCode;
			}
		}
		return OBJECT;
	}

	public static TypeCode forClass(Class<?> clazz) {
		for (TypeCode typeCode : values()) {
			if (typeCode.getType() == clazz) {
				return typeCode;
			}
		}
		return OBJECT;
	}

}
