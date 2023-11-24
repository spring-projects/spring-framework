/*
 * Copyright 2002-2023 the original author or authors.
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
	BOOLEAN(Boolean.TYPE),

	/**
	 * A {@code char}.
	 */
	CHAR(Character.TYPE),

	/**
	 * A {@code byte}.
	 */
	BYTE(Byte.TYPE),

	/**
	 * A {@code short}.
	 */
	SHORT(Short.TYPE),

	/**
	 * An {@code int}.
	 */
	INT(Integer.TYPE),

	/**
	 * A {@code long}.
	 */
	LONG(Long.TYPE),

	/**
	 * A {@code float}.
	 */
	FLOAT(Float.TYPE),

	/**
	 * A {@code double}.
	 */
	DOUBLE(Double.TYPE);


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
