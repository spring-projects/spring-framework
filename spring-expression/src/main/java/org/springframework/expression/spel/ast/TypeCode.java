/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
