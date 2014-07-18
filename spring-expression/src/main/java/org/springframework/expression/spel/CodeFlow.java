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

package org.springframework.expression.spel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Stack;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.util.Assert;

/**
 * Records intermediate compilation state as the bytecode is generated for a parsed
 * expression. Also contains bytecode generation helper functions.
 *
 * @author Andy Clement
 * @since 4.1
 */
public class CodeFlow implements Opcodes {

	/**
	 * Record the type of what is on top of the bytecode stack (i.e. the type of the
	 * output from the previous expression component). New scopes are used to evaluate
	 * sub-expressions like the expressions for the argument values in a method invocation
	 * expression.
	 */
	private final Stack<ArrayList<String>> compilationScopes;


	public CodeFlow() {
		this.compilationScopes = new Stack<ArrayList<String>>();
		this.compilationScopes.add(new ArrayList<String>());
	}


	/**
	 * Push the byte code to load the target (i.e. what was passed as the first argument
	 * to CompiledExpression.getValue(target, context))
	 * @param mv the visitor into which the load instruction should be inserted
	 */
	public void loadTarget(MethodVisitor mv) {
		mv.visitVarInsn(ALOAD, 1);
	}

	/**
	 * Record the descriptor for the most recently evaluated expression element.
	 * @param descriptor type descriptor for most recently evaluated element
	 */
	public void pushDescriptor(String descriptor) {
		Assert.notNull(descriptor);
		this.compilationScopes.peek().add(descriptor);
	}

	/**
	 * Enter a new compilation scope, usually due to nested expression evaluation. For
	 * example when the arguments for a method invocation expression are being evaluated,
	 * each argument will be evaluated in a new scope.
	 */
	public void enterCompilationScope() {
		this.compilationScopes.push(new ArrayList<String>());
	}

	/**
	 * Exit a compilation scope, usually after a nested expression has been evaluated. For
	 * example after an argument for a method invocation has been evaluated this method
	 * returns us to the previous (outer) scope.
	 */
	public void exitCompilationScope() {
		this.compilationScopes.pop();
	}

	/**
	 * @return the descriptor for the item currently on top of the stack (in the current scope)
	 */
	public String lastDescriptor() {
		if (this.compilationScopes.peek().size() == 0) {
			return null;
		}
		return this.compilationScopes.peek().get(this.compilationScopes.peek().size() - 1);
	}

	/**
	 * If the codeflow shows the last expression evaluated to java.lang.Boolean then
	 * insert the necessary instructions to unbox that to a boolean primitive.
	 * @param mv the visitor into which new instructions should be inserted
	 */
	public void unboxBooleanIfNecessary(MethodVisitor mv) {
		if (lastDescriptor().equals("Ljava/lang/Boolean")) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
		}
	}


	/**
	 * Insert any necessary cast and value call to convert from a boxed type to a
	 * primitive value
	 * @param mv the method visitor into which instructions should be inserted
	 * @param ch the primitive type desired as output
	 * @param isObject indicates whether the type on the stack is being thought of
	 * as Object (and so requires a cast)
	 */
	public static void insertUnboxInsns(MethodVisitor mv, char ch, boolean isObject) {
		switch (ch) {
		case 'I':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
			break;
		case 'Z':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
			break;
		case 'B':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
			break;
		case 'C':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
			break;
		case 'D':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
			break;
		case 'S':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
			break;
		case 'F':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
			break;
		case 'J':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
			break;
		default:
			throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + ch + "'");
		}
	}

	/**
	 * Create the JVM signature descriptor for a method. This consists of the descriptors
	 * for the constructor parameters surrounded with parentheses, followed by the
	 * descriptor for the return type. Note the descriptors here are JVM descriptors,
	 * unlike the other descriptor forms the compiler is using which do not include the
	 * trailing semicolon.
	 * @param method the method
	 * @return a String signature descriptor (e.g. "(ILjava/lang/String;)V")
	 */
	public static String createSignatureDescriptor(Method method) {
		Class<?>[] params = method.getParameterTypes();
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Class<?> param : params) {
			sb.append(toJVMDescriptor(param));
		}
		sb.append(")");
		sb.append(toJVMDescriptor(method.getReturnType()));
		return sb.toString();
	}

	/**
	 * Create the JVM signature descriptor for a constructor. This consists of the
	 * descriptors for the constructor parameters surrounded with parentheses. Note the
	 * descriptors here are JVM descriptors, unlike the other descriptor forms the
	 * compiler is using which do not include the trailing semicolon.
	 * @param ctor the constructor
	 * @return a String signature descriptor (e.g. "(ILjava/lang/String;)")
	 */
	public static String createSignatureDescriptor(Constructor<?> ctor) {
		Class<?>[] params = ctor.getParameterTypes();
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Class<?> param : params) {
			sb.append(toJVMDescriptor(param));
		}
		sb.append(")V");
		return sb.toString();
	}

	/**
	 * Determine the JVM descriptor for a specified class. Unlike the other descriptors
	 * used in the compilation process, this is the one the JVM wants, so this one
	 * includes any necessary trailing semicolon (e.g. Ljava/lang/String; rather than
	 * Ljava/lang/String)
	 * @param clazz a class
	 * @return the JVM descriptor for the class
	 */
	public static String toJVMDescriptor(Class<?> clazz) {
		StringBuilder sb = new StringBuilder();
		if (clazz.isArray()) {
			while (clazz.isArray()) {
				sb.append("[");
				clazz = clazz.getComponentType();
			}
		}
		if (clazz.isPrimitive()) {
			if (clazz == Void.TYPE) {
				sb.append('V');
			}
			else if (clazz == Integer.TYPE) {
				sb.append('I');
			}
			else if (clazz == Boolean.TYPE) {
				sb.append('Z');
			}
			else if (clazz == Character.TYPE) {
				sb.append('C');
			}
			else if (clazz == Long.TYPE) {
				sb.append('J');
			}
			else if (clazz == Double.TYPE) {
				sb.append('D');
			}
			else if (clazz == Float.TYPE) {
				sb.append('F');
			}
			else if (clazz == Byte.TYPE) {
				sb.append('B');
			}
			else if (clazz == Short.TYPE) {
				sb.append('S');
			}
		}
		else {
			sb.append("L");
			sb.append(clazz.getName().replace('.', '/'));
			sb.append(";");
		}
		return sb.toString();
	}

	/**
	 * Determine the descriptor for an object instance (or {@code null}).
	 * @param value an object (possibly {@code null})
	 * @return the type descriptor for the object
	 * (descriptor is "Ljava/lang/Object" for {@code null} value)
	 */
	public static String toDescriptorFromObject(Object value) {
		if (value == null) {
			return "Ljava/lang/Object";
		}
		else {
			return toDescriptor(value.getClass());
		}
	}

	/**
	 * @param descriptor type descriptor
	 * @return {@code true} if the descriptor is for a boolean primitive or boolean reference type
	 */
	public static boolean isBooleanCompatible(String descriptor) {
		return (descriptor != null && (descriptor.equals("Z") || descriptor.equals("Ljava/lang/Boolean")));
	}

	/**
	 * @param descriptor type descriptor
	 * @return {@code true} if the descriptor is for a primitive type
	 */
	public static boolean isPrimitive(String descriptor) {
		return (descriptor != null && descriptor.length() == 1);
	}

	/**
	 * @param descriptor the descriptor for a possible primitive array
	 * @return {@code true} if the descriptor is for a primitive array (e.g. "[[I")
	 */
	public static boolean isPrimitiveArray(String descriptor) {
		boolean primitive = true;
		for (int i = 0, max = descriptor.length(); i < max; i++) {
			char ch = descriptor.charAt(i);
			if (ch == '[') {
				continue;
			}
			primitive = (ch != 'L');
			break;
		}
		return primitive;
	}

	/**
	 * Determine if boxing/unboxing can get from one type to the other. Assumes at least
	 * one of the types is in boxed form (i.e. single char descriptor).
	 * @return {@code true} if it is possible to get (via boxing) from one descriptor to the other
	 */
	public static boolean areBoxingCompatible(String desc1, String desc2) {
		if (desc1.equals(desc2)) {
			return true;
		}
		if (desc1.length() == 1) {
			if (desc1.equals("D")) {
				return desc2.equals("Ljava/lang/Double");
			}
			else if (desc1.equals("F")) {
				return desc2.equals("Ljava/lang/Float");
			}
			else if (desc1.equals("J")) {
				return desc2.equals("Ljava/lang/Long");
			}
			else if (desc1.equals("I")) {
				return desc2.equals("Ljava/lang/Integer");
			}
			else if (desc1.equals("Z")) {
				return desc2.equals("Ljava/lang/Boolean");
			}
		}
		else if (desc2.length() == 1) {
			if (desc2.equals("D")) {
				return desc1.equals("Ljava/lang/Double");
			}
			else if (desc2.equals("F")) {
				return desc1.equals("Ljava/lang/Float");
			}
			else if (desc2.equals("J")) {
				return desc1.equals("Ljava/lang/Long");
			}
			else if (desc2.equals("I")) {
				return desc1.equals("Ljava/lang/Integer");
			}
			else if (desc2.equals("Z")) {
				return desc1.equals("Ljava/lang/Boolean");
			}
		}
		return false;
	}

	/**
	 * Determine if the supplied descriptor is for a supported number type or boolean. The
	 * compilation process only (currently) supports certain number types. These are
	 * double, float, long and int.
	 * @param descriptor the descriptor for a type
	 * @return {@code true} if the descriptor is for a supported numeric type or boolean
	 */
	public static boolean isPrimitiveOrUnboxableSupportedNumberOrBoolean(String descriptor) {
		if (descriptor == null) {
			return false;
		}
		if (descriptor.length( )== 1) {
			return ("DFJZI".indexOf(descriptor.charAt(0)) != -1);
		}
		if (descriptor.startsWith("Ljava/lang/")) {
			if (descriptor.equals("Ljava/lang/Double") || descriptor.equals("Ljava/lang/Integer") ||
				descriptor.equals("Ljava/lang/Float") || descriptor.equals("Ljava/lang/Long") ||
				descriptor.equals("Ljava/lang/Boolean")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if the supplied descriptor is for a supported number. The compilation
	 * process only (currently) supports certain number types. These are double, float,
	 * long and int.
	 * @param descriptor the descriptor for a type
	 * @return {@code true} if the descriptor is for a supported numeric type
	 */
	public static boolean isPrimitiveOrUnboxableSupportedNumber(String descriptor) {
		if (descriptor == null) {
			return false;
		}
		if (descriptor.length() == 1) {
			return ("DFJI".indexOf(descriptor.charAt(0)) != -1);
		}
		if (descriptor.startsWith("Ljava/lang/")) {
			if (descriptor.equals("Ljava/lang/Double") || descriptor.equals("Ljava/lang/Integer") ||
				descriptor.equals("Ljava/lang/Float") || descriptor.equals("Ljava/lang/Long")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param descriptor a descriptor for a type that should have a primitive representation
	 * @return the single character descriptor for a primitive input descriptor
	 */
	public static char toPrimitiveTargetDesc(String descriptor) {
		if (descriptor.length() == 1) {
			return descriptor.charAt(0);
		}
		if (descriptor.equals("Ljava/lang/Double")) {
			return 'D';
		}
		else if (descriptor.equals("Ljava/lang/Integer")) {
			return 'I';
		}
		else if (descriptor.equals("Ljava/lang/Float")) {
			return 'F';
		}
		else if (descriptor.equals("Ljava/lang/Long")) {
			return 'J';
		}
		else if (descriptor.equals("Ljava/lang/Boolean")) {
			return 'Z';
		}
		else {
			throw new IllegalStateException("No primitive for '"+descriptor+"'");
		}
	}

	/**
	 * Insert the appropriate CHECKCAST instruction for the supplied descriptor.
	 * @param mv the target visitor into which the instruction should be inserted
	 * @param descriptor the descriptor of the type to cast to
	 */
	public static void insertCheckCast(MethodVisitor mv, String descriptor) {
		if (descriptor.length() != 1) {
			if (descriptor.charAt(0) == '[') {
				if (isPrimitiveArray(descriptor)) {
					mv.visitTypeInsn(CHECKCAST, descriptor);
				}
				else {
					mv.visitTypeInsn(CHECKCAST, descriptor + ";");
				}
			}
			else {
				// This is chopping off the 'L' to leave us with "java/lang/String"
				mv.visitTypeInsn(CHECKCAST, descriptor.substring(1));
			}
		}
	}

	/**
	 * Determine the appropriate boxing instruction for a specific type (if it needs
	 * boxing) and insert the instruction into the supplied visitor.
	 * @param mv the target visitor for the new instructions
	 * @param descriptor the descriptor of a type that may or may not need boxing
	 */
	public static void insertBoxIfNecessary(MethodVisitor mv, String descriptor) {
		if (descriptor.length() == 1) {
			insertBoxIfNecessary(mv, descriptor.charAt(0));
		}
	}

	/**
	 * Determine the appropriate boxing instruction for a specific type (if it needs
	 * boxing) and insert the instruction into the supplied visitor.
	 * @param mv the target visitor for the new instructions
	 * @param ch the descriptor of the type that might need boxing
	 */
	public static void insertBoxIfNecessary(MethodVisitor mv, char ch) {
		switch (ch) {
		case 'I':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
			break;
		case 'F':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
			break;
		case 'S':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
			break;
		case 'Z':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
			break;
		case 'J':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
			break;
		case 'D':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
			break;
		case 'C':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
			break;
		case 'B':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
			break;
		case 'L':
			case 'V':
		case '[':
			// no box needed
			break;
		default:
			throw new IllegalArgumentException("Boxing should not be attempted for descriptor '" + ch + "'");
		}
	}

	/**
	 * Deduce the descriptor for a type. Descriptors are like JVM type names but missing
	 * the trailing ';' so for Object the descriptor is "Ljava/lang/Object" for int it is
	 * "I".
	 * @param type the type (may be primitive) for which to determine the descriptor
	 * @return the descriptor
	 */
	public static String toDescriptor(Class<?> type) {
		String name = type.getName();
		if (type.isPrimitive()) {
			switch (name.length()) {
				case 3:
					return "I";
				case 4:
					if (name.equals("long")) {
						return "J";
					}
					else if (name.equals("char")) {
						return "C";
					}
					else if (name.equals("byte")) {
						return "B";
					}
					else if (name.equals("void")) {
						return "V";
					}
					break;
				case 5:
					if (name.equals("float")) {
						return "F";
					}
					else if (name.equals("short")) {
						return "S";
					}
					break;
				case 6:
					if (name.equals("double")) {
						return "D";
					}
					break;
				case 7:
					if (name.equals("boolean")) {
						return "Z";
					}
					break;
			}
		}
		else {
			if (name.charAt(0) != '[') {
				return "L" + type.getName().replace('.', '/');
			}
			else {
				if (name.endsWith(";")) {
					return name.substring(0, name.length() - 1).replace('.', '/');
				}
				else {
					return name;  // array has primitive component type
				}
			}
		}
		return null;
	}

	/**
	 * Create an array of descriptors representing the parameter types for the supplied
	 * method. Returns a zero sized array if there are no parameters.
	 * @param method a Method
	 * @return a String array of descriptors, one entry for each method parameter
	 */
	public static String[] toParamDescriptors(Method method) {
		return toDescriptors(method.getParameterTypes());
	}

	/**
	 * Create an array of descriptors representing the parameter types for the supplied
	 * constructor. Returns a zero sized array if there are no parameters.
	 * @param ctor a Constructor
	 * @return a String array of descriptors, one entry for each constructor parameter
	 */
	public static String[] toParamDescriptors(Constructor<?> ctor) {
		return toDescriptors(ctor.getParameterTypes());
	}

	private static String[] toDescriptors(Class<?>[] types) {
		int typesCount = types.length;
		String[] descriptors = new String[typesCount];
		for (int p = 0; p < typesCount; p++) {
			descriptors[p] = toDescriptor(types[p]);
		}
		return descriptors;
	}

}
