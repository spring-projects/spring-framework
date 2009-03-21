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

package org.springframework.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.asm.ClassReader;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.asm.commons.EmptyVisitor;
import org.springframework.util.ClassUtils;

/**
 * Implementation of {@link ParameterNameDiscoverer} that uses the LocalVariableTable
 * information in the method attributes to discover parameter names. Returns
 * <code>null</code> if the class file was compiled without debug information.
 *
 * <p>Uses ObjectWeb's ASM library for analyzing class files. Each discoverer
 * instance caches the ASM ClassReader for each introspected Class, in a
 * thread-safe manner. It is recommended to reuse discoverer instances
 * as far as possible.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public class LocalVariableTableParameterNameDiscoverer implements ParameterNameDiscoverer {

	private static Log logger = LogFactory.getLog(LocalVariableTableParameterNameDiscoverer.class);

	private final Map<Member, String[]> parameterNamesCache = new ConcurrentHashMap<Member, String[]>();

	private final Map<Class, ClassReader> classReaderCache = new HashMap<Class, ClassReader>();


	public String[] getParameterNames(Method method) {
		String[] paramNames = this.parameterNamesCache.get(method);
		if (paramNames == null) {
			try {
				paramNames = visitMethod(method).getParameterNames();
				if (paramNames != null) {
					this.parameterNamesCache.put(method, paramNames);
				}
			}
			catch (IOException ex) {
				// We couldn't load the class file, which is not fatal as it
				// simply means this method of discovering parameter names won't work.
				if (logger.isDebugEnabled()) {
					logger.debug("IOException whilst attempting to read '.class' file for class [" +
							method.getDeclaringClass().getName() +
							"] - unable to determine parameter names for method: " + method, ex);
				}
			}
		}
		return paramNames;
	}

	public String[] getParameterNames(Constructor ctor) {
		String[] paramNames = this.parameterNamesCache.get(ctor);
		if (paramNames == null) {
			try {
				paramNames = visitConstructor(ctor).getParameterNames();
				if (paramNames != null) {
					this.parameterNamesCache.put(ctor, paramNames);
				}
			}
			catch (IOException ex) {
				// We couldn't load the class file, which is not fatal as it
				// simply means this method of discovering parameter names won't work.
				if (logger.isDebugEnabled()) {
					logger.debug("IOException whilst attempting to read '.class' file for class [" +
							ctor.getDeclaringClass().getName() +
							"] - unable to determine parameter names for constructor: " + ctor, ex);
				}
			}
		}
		return paramNames;
	}

	/**
	 * Visit the given method and discover its parameter names.
	 */
	private ParameterNameDiscoveringVisitor visitMethod(Method method) throws IOException {
		ClassReader classReader = getClassReader(method.getDeclaringClass());
		FindMethodParameterNamesClassVisitor classVisitor = new FindMethodParameterNamesClassVisitor(method);
		classReader.accept(classVisitor, false);
		return classVisitor;
	}

	/**
	 * Visit the given constructor and discover its parameter names.
	 */
	private ParameterNameDiscoveringVisitor visitConstructor(Constructor ctor) throws IOException {
		ClassReader classReader = getClassReader(ctor.getDeclaringClass());
		FindConstructorParameterNamesClassVisitor classVisitor = new FindConstructorParameterNamesClassVisitor(ctor);
		classReader.accept(classVisitor, false);
		return classVisitor;
	}

	/**
	 * Obtain a (cached) ClassReader for the given class.
	 */
	private ClassReader getClassReader(Class clazz) throws IOException {
		synchronized (this.classReaderCache) {
			ClassReader classReader = (ClassReader) this.classReaderCache.get(clazz);
			if (classReader == null) {
				InputStream is = clazz.getResourceAsStream(ClassUtils.getClassFileName(clazz));
				if (is == null) {
					throw new FileNotFoundException("Class file for class [" + clazz.getName() + "] not found");
				}
				try {
					classReader = new ClassReader(is);
					this.classReaderCache.put(clazz, classReader);
				}
				finally {
					is.close();
				}
			}
			return classReader;
		}
	}


	/**
	 * Helper class that looks for a given member name and descriptor, and then
	 * attempts to find the parameter names for that member.
	 */
	private static abstract class ParameterNameDiscoveringVisitor extends EmptyVisitor {

		private String methodNameToMatch;

		private String descriptorToMatch;

		private int numParamsExpected;
		
		/*
		 * The nth entry contains the slot index of the LVT table entry holding the
		 * argument name for the nth parameter.
		 */
		private int[] lvtSlotIndex;

		private String[] parameterNames;
		
		public ParameterNameDiscoveringVisitor(String name, boolean isStatic, Class[] paramTypes) {
			this.methodNameToMatch = name;
			this.numParamsExpected = paramTypes.length;
			computeLvtSlotIndices(isStatic, paramTypes);
		}
		
		public void setDescriptorToMatch(String descriptor) {
			this.descriptorToMatch = descriptor;			
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			if (name.equals(this.methodNameToMatch) && desc.equals(this.descriptorToMatch)) {
				return new LocalVariableTableVisitor(this, isStatic(access));
			} 
			else {
				// Not interested in this method...
				return null;
			}
		}
		
		private boolean isStatic(int access) {
			return ((access & Opcodes.ACC_STATIC) > 0);
		}

		public String[] getParameterNames() {
			return this.parameterNames;
		}

		private void computeLvtSlotIndices(boolean isStatic, Class[] paramTypes) {
			this.lvtSlotIndex = new int[paramTypes.length];
			int nextIndex = (isStatic ? 0 : 1);
			for (int i = 0; i < paramTypes.length; i++) {
				this.lvtSlotIndex[i] = nextIndex;
				if (isWideType(paramTypes[i])) {
					nextIndex += 2;
				}
				else {
					nextIndex++;
				}
			}
		}
		
		private boolean isWideType(Class aType) {
			return (aType == Long.TYPE || aType == Double.TYPE);
		}
	}


	private static class FindMethodParameterNamesClassVisitor extends ParameterNameDiscoveringVisitor {
		
		public FindMethodParameterNamesClassVisitor(Method method) {
			super(method.getName(), Modifier.isStatic(method.getModifiers()), method.getParameterTypes());
			setDescriptorToMatch(Type.getMethodDescriptor(method));
		}
	}


	private static class FindConstructorParameterNamesClassVisitor extends ParameterNameDiscoveringVisitor {
		
		public FindConstructorParameterNamesClassVisitor(Constructor ctor) {
			super("<init>", false, ctor.getParameterTypes());
			Type[] pTypes = new Type[ctor.getParameterTypes().length];
			for (int i = 0; i < pTypes.length; i++) {
				pTypes[i] = Type.getType(ctor.getParameterTypes()[i]);
			}
			setDescriptorToMatch(Type.getMethodDescriptor(Type.VOID_TYPE, pTypes));
		}
	}


	private static class LocalVariableTableVisitor extends EmptyVisitor {

		private final ParameterNameDiscoveringVisitor memberVisitor;

		private final boolean isStatic;

		private String[] parameterNames;

		private boolean hasLvtInfo = false;

		public LocalVariableTableVisitor(ParameterNameDiscoveringVisitor memberVisitor, boolean isStatic) {
			this.memberVisitor = memberVisitor;
			this.isStatic = isStatic;
			this.parameterNames = new String[memberVisitor.numParamsExpected];
		}

		@Override
		public void visitLocalVariable(
				String name, String description, String signature, Label start, Label end, int index) {
			this.hasLvtInfo = true;
			int[] lvtSlotIndices = this.memberVisitor.lvtSlotIndex;
			for (int i = 0; i < lvtSlotIndices.length; i++) {
				if (lvtSlotIndices[i] == index) {
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
				this.memberVisitor.parameterNames = this.parameterNames;
			}
		}
	}

}
