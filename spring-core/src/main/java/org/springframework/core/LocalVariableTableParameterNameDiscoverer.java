/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Implementation of {@link ParameterNameDiscoverer} that uses the LocalVariableTable
 * information in the method attributes to discover parameter names. Returns
 * {@code null} if the class file was compiled without debug information.
 *
 * <p>Uses ObjectWeb's ASM library for analyzing class files. Each discoverer instance
 * caches the ASM discovered information for each introspected Class, in a thread-safe
 * manner. It is recommended to reuse ParameterNameDiscoverer instances as far as possible.
 *
 * @author Adrian Colyer
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 */
public class LocalVariableTableParameterNameDiscoverer implements ParameterNameDiscoverer {

	private static final Log logger = LogFactory.getLog(LocalVariableTableParameterNameDiscoverer.class);

	// marker object for classes that do not have any debug info
	private static final Map<Member, String[]> NO_DEBUG_INFO_MAP = Collections.emptyMap();

	// the cache uses a nested index (value is a map) to keep the top level cache relatively small in size
	private final Map<Class<?>, Map<Member, String[]>> parameterNamesCache = new ConcurrentHashMap<>(32);


	@Override
	@Nullable
	public String[] getParameterNames(Method method) {
		Method originalMethod = BridgeMethodResolver.findBridgedMethod(method);
		Class<?> declaringClass = originalMethod.getDeclaringClass();
		Map<Member, String[]> map = this.parameterNamesCache.get(declaringClass);
		if (map == null) {
			map = inspectClass(declaringClass);
			this.parameterNamesCache.put(declaringClass, map);
		}
		if (map != NO_DEBUG_INFO_MAP) {
			return map.get(originalMethod);
		}
		return null;
	}

	@Override
	@Nullable
	public String[] getParameterNames(Constructor<?> ctor) {
		Class<?> declaringClass = ctor.getDeclaringClass();
		Map<Member, String[]> map = this.parameterNamesCache.get(declaringClass);
		if (map == null) {
			map = inspectClass(declaringClass);
			this.parameterNamesCache.put(declaringClass, map);
		}
		if (map != NO_DEBUG_INFO_MAP) {
			return map.get(ctor);
		}
		return null;
	}

	/**
	 * Inspects the target class. Exceptions will be logged and a maker map returned
	 * to indicate the lack of debug information.
	 */
	private Map<Member, String[]> inspectClass(Class<?> clazz) {
		InputStream is = clazz.getResourceAsStream(ClassUtils.getClassFileName(clazz));
		if (is == null) {
			// We couldn't load the class file, which is not fatal as it
			// simply means this method of discovering parameter names won't work.
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot find '.class' file for class [" + clazz +
						"] - unable to determine constructor/method parameter names");
			}
			return NO_DEBUG_INFO_MAP;
		}
		try {
			ClassReader classReader = new ClassReader(is);
			Map<Member, String[]> map = new ConcurrentHashMap<>(32);
			classReader.accept(new ParameterNameDiscoveringVisitor(clazz, map), 0);
			return map;
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Exception thrown while reading '.class' file for class [" + clazz +
						"] - unable to determine constructor/method parameter names", ex);
			}
		}
		catch (IllegalArgumentException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("ASM ClassReader failed to parse class file [" + clazz +
						"], probably due to a new Java class file version that isn't supported yet " +
						"- unable to determine constructor/method parameter names", ex);
			}
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ex) {
				// ignore
			}
		}
		return NO_DEBUG_INFO_MAP;
	}


	/**
	 * Helper class that inspects all methods (constructor included) and then
	 * attempts to find the parameter names for that member.
	 */
	private static class ParameterNameDiscoveringVisitor extends ClassVisitor {

		private static final String STATIC_CLASS_INIT = "<clinit>";

		private final Class<?> clazz;

		private final Map<Member, String[]> memberMap;

		public ParameterNameDiscoveringVisitor(Class<?> clazz, Map<Member, String[]> memberMap) {
			super(SpringAsmInfo.ASM_VERSION);
			this.clazz = clazz;
			this.memberMap = memberMap;
		}

		@Override
		@Nullable
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			// exclude synthetic + bridged && static class initialization
			if (!isSyntheticOrBridged(access) && !STATIC_CLASS_INIT.equals(name)) {
				return new LocalVariableTableVisitor(clazz, memberMap, name, desc, isStatic(access));
			}
			return null;
		}

		private static boolean isSyntheticOrBridged(int access) {
			return (((access & Opcodes.ACC_SYNTHETIC) | (access & Opcodes.ACC_BRIDGE)) > 0);
		}

		private static boolean isStatic(int access) {
			return ((access & Opcodes.ACC_STATIC) > 0);
		}
	}


	private static class LocalVariableTableVisitor extends MethodVisitor {

		private static final String CONSTRUCTOR = "<init>";

		private final Class<?> clazz;

		private final Map<Member, String[]> memberMap;

		private final String name;

		private final Type[] args;

		private final String[] parameterNames;

		private final boolean isStatic;

		private boolean hasLvtInfo = false;

		/*
		 * The nth entry contains the slot index of the LVT table entry holding the
		 * argument name for the nth parameter.
		 */
		private final int[] lvtSlotIndex;

		public LocalVariableTableVisitor(Class<?> clazz, Map<Member, String[]> map, String name, String desc, boolean isStatic) {
			super(SpringAsmInfo.ASM_VERSION);
			this.clazz = clazz;
			this.memberMap = map;
			this.name = name;
			this.args = Type.getArgumentTypes(desc);
			this.parameterNames = new String[this.args.length];
			this.isStatic = isStatic;
			this.lvtSlotIndex = computeLvtSlotIndices(isStatic, this.args);
		}

		@Override
		public void visitLocalVariable(String name, String description, String signature, Label start, Label end, int index) {
			this.hasLvtInfo = true;
			for (int i = 0; i < this.lvtSlotIndex.length; i++) {
				if (this.lvtSlotIndex[i] == index) {
					this.parameterNames[i] = name;
				}
			}
		}

		@Override
		public void visitEnd() {
			if (this.hasLvtInfo || (this.isStatic && this.parameterNames.length == 0)) {
				// visitLocalVariable will never be called for static no args methods
				// which doesn't use any local variables.
				// This means that hasLvtInfo could be false for that kind of methods
				// even if the class has local variable info.
				this.memberMap.put(resolveMember(), this.parameterNames);
			}
		}

		private Member resolveMember() {
			ClassLoader loader = this.clazz.getClassLoader();
			Class<?>[] argTypes = new Class<?>[this.args.length];
			for (int i = 0; i < this.args.length; i++) {
				argTypes[i] = ClassUtils.resolveClassName(this.args[i].getClassName(), loader);
			}
			try {
				if (CONSTRUCTOR.equals(this.name)) {
					return this.clazz.getDeclaredConstructor(argTypes);
				}
				return this.clazz.getDeclaredMethod(this.name, argTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Method [" + this.name +
						"] was discovered in the .class file but cannot be resolved in the class object", ex);
			}
		}

		private static int[] computeLvtSlotIndices(boolean isStatic, Type[] paramTypes) {
			int[] lvtIndex = new int[paramTypes.length];
			int nextIndex = (isStatic ? 0 : 1);
			for (int i = 0; i < paramTypes.length; i++) {
				lvtIndex[i] = nextIndex;
				if (isWideType(paramTypes[i])) {
					nextIndex += 2;
				}
				else {
					nextIndex++;
				}
			}
			return lvtIndex;
		}

		private static boolean isWideType(Type aType) {
			// float is not a wide type
			return (aType == Type.LONG_TYPE || aType == Type.DOUBLE_TYPE);
		}
	}

}
