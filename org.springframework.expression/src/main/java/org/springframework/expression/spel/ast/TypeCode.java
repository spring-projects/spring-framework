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

package org.springframework.expression.spel.ast;

enum TypeCode {

	OBJECT(0, Object.class), BOOLEAN(1, Boolean.TYPE), BYTE(1, Byte.TYPE), CHAR(1, Character.TYPE), SHORT(2, Short.TYPE), INT(
			3, Integer.TYPE), LONG(4, Long.TYPE), FLOAT(5, Float.TYPE), DOUBLE(6, Double.TYPE);

	private int code;
	private Class<?> type;

	TypeCode(int code, Class<?> type) {
		this.code = code;
		this.type = type;
	}

	public Class<?> getType() {
		return type;
	}

	public static TypeCode forClass(Class<?> c) {
		TypeCode[] allValues = TypeCode.values();
		for (int i = 0; i < allValues.length; i++) {
			TypeCode typeCode = allValues[i];
			if (c == typeCode.getType()) {
				return typeCode;
			}
		}
		return OBJECT;
	}

	/**
	 * For a primitive name this will determine the typecode value - supports
	 * int,byte,char,short,long,double,float,boolean
	 */
	public static TypeCode forName(String name) {
		if (name.equals("int"))
			return TypeCode.INT;
		else if (name.equals("boolean"))
			return TypeCode.BOOLEAN;
		else if (name.equals("char"))
			return TypeCode.CHAR;
		else if (name.equals("long"))
			return TypeCode.LONG;
		else if (name.equals("float"))
			return TypeCode.FLOAT;
		else if (name.equals("double"))
			return TypeCode.DOUBLE;
		else if (name.equals("short"))
			return TypeCode.SHORT;
		else if (name.equals("byte"))
			return TypeCode.BYTE;
		return TypeCode.OBJECT;
	}

	public int getCode() {
		return code;
	}

	public Object coerce(TypeCode fromTypeCode, Object fromObject) {
		if (this == TypeCode.INT) {
			switch (fromTypeCode) {
			case BOOLEAN:
				return ((Boolean) fromObject).booleanValue() ? 1 : 0;
			}
		}
		//			
		// return Integer.valueOf
		// } else if (this==TypeCode.BOOLEAN) {
		// return new Boolean(left).intValue();
		return null;
	}

	public static TypeCode forValue(Number op1) {
		return forClass(op1.getClass());
	}

	public boolean isDouble() {
		return this == DOUBLE;
	}

	public boolean isFloat() {
		return this == FLOAT;
	}

	public boolean isLong() {
		return this == LONG;
	}
}
