/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.reflect.Array;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.standard.CodeFlow;

/**
 * Represents a reference to a type, for example "T(String)" or "T(com.somewhere.Foo)"
 *
 * @author Andy Clement
 */
public class TypeReference extends SpelNodeImpl {

	private final int dimensions;

	private transient Class<?> type;

	public TypeReference(int pos, SpelNodeImpl qualifiedId) {
		this(pos,qualifiedId,0);
	}

	public TypeReference(int pos, SpelNodeImpl qualifiedId, int dims) {
		super(pos,qualifiedId);
		this.dimensions = dims;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		// TODO possible optimization here if we cache the discovered type reference, but can we do that?
		String typename = (String) this.children[0].getValueInternal(state).getValue();
		if (typename.indexOf(".") == -1 && Character.isLowerCase(typename.charAt(0))) {
			TypeCode tc = TypeCode.valueOf(typename.toUpperCase());
			if (tc != TypeCode.OBJECT) {
				// it is a primitive type
				Class<?> clazz = tc.getType();
				clazz = makeArrayIfNecessary(clazz);
				this.exitTypeDescriptor = "Ljava/lang/Class";
				this.type = clazz;
				return new TypedValue(clazz);
			}
		}
		Class<?> clazz = state.findType(typename);
		clazz = makeArrayIfNecessary(clazz);
		this.exitTypeDescriptor = "Ljava/lang/Class";
		this.type = clazz;
		return new TypedValue(clazz);
	}

	private Class<?> makeArrayIfNecessary(Class<?> clazz) {
		if (this.dimensions!=0) {
			for (int i=0;i<this.dimensions;i++) {
				Object o = Array.newInstance(clazz, 0);
				clazz = o.getClass();
			}
		}
		return clazz;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("T(");
		sb.append(getChild(0).toStringAST());
		for (int d=0;d<this.dimensions;d++) {
			sb.append("[]");
		}
		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public boolean isCompilable() {
		return this.exitTypeDescriptor != null;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		// TODO Future optimization - if followed by a static method call, skip generating code here
		if (type.isPrimitive()) {
			if (type == Integer.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
			} else if (type == Boolean.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
			} else if (type == Byte.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
			} else if (type == Short.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
			} else if (type == Double.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
			} else if (type == Character.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
			} else if (type == Float.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
			} else if (type == Long.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
			} else if (type == Boolean.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
	        }
		}
		else {
			mv.visitLdcInsn(Type.getType(type));
		}
		codeflow.pushDescriptor(getExitDescriptor());
	}

}
