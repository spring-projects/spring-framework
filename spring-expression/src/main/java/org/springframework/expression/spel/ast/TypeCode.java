/*
 * Copyright 2002-2018 the original author or authors.
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
 * Captures primitive types and their corresponding class objects, plus one special entry
 * that represents all reference (non-primitive) types.
 *
 * @author Andy Clement
 */
public enum TypeCode {

	OBJECT(Object.class),

	BOOLEAN(Boolean.TYPE),

	BYTE(Byte.TYPE),

	CHAR(Character.TYPE),

	DOUBLE(Double.TYPE),

	FLOAT(Float.TYPE),

	INT(Integer.TYPE),

	LONG(Long.TYPE),

	SHORT(Short.TYPE);


	private Class<?> type;


	TypeCode(Class<?> type) {
		this.type = type;
	}


	public Class<?> getType() {
		return this.type;
	}


	public static TypeCode forName(String name) {
		String searchingFor = name.toUpperCase();
		TypeCode[] tcs = values();
		for (int i = 1; i < tcs.length; i++) {
			if (tcs[i].name().equals(searchingFor)) {
				return tcs[i];
			}
		}
		return OBJECT;
	}

	public static TypeCode forClass(Class<?> clazz) {
		TypeCode[] allValues = TypeCode.values();
		for (TypeCode typeCode : allValues) {
			if (clazz == typeCode.getType()) {
				return typeCode;
			}
		}
		return OBJECT;
	}

}
