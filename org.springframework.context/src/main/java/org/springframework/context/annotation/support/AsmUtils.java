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

package org.springframework.context.annotation.support;

import static java.lang.String.*;
import static org.springframework.util.ClassUtils.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.asm.ClassReader;
import org.springframework.asm.commons.EmptyVisitor;
import org.springframework.util.ClassUtils;


/**
 * Various utility methods commonly used when interacting with ASM, classloading
 * and creating {@link MutableAnnotation} instances.
 * 
 * @author Chris Beams
 */
class AsmUtils {

	public static final EmptyVisitor ASM_EMPTY_VISITOR = new EmptyVisitor();

	private static final Log log = LogFactory.getLog(AsmUtils.class);

	/**
	 * Convert a type descriptor to a classname suitable for classloading with
	 * Class.forName().
	 * 
	 * @param typeDescriptor see ASM guide section 2.1.3
	 */
	public static String convertAsmTypeDescriptorToClassName(String typeDescriptor) {
		final String internalName; // See ASM guide section 2.1.2

		if ("V".equals(typeDescriptor))
			return Void.class.getName();
		if ("I".equals(typeDescriptor))
			return Integer.class.getName();
		if ("Z".equals(typeDescriptor))
			return Boolean.class.getName();

		// strip the leading array/object/primitive identifier
		if (typeDescriptor.startsWith("[["))
			internalName = typeDescriptor.substring(3);
		else if (typeDescriptor.startsWith("["))
			internalName = typeDescriptor.substring(2);
		else
			internalName = typeDescriptor.substring(1);

		// convert slashes to dots
		String className = internalName.replace('/', '.');

		// and strip trailing semicolon (if present)
		if (className.endsWith(";"))
			className = className.substring(0, internalName.length() - 1);

		return className;
	}

	/**
	 * @param methodDescriptor see ASM guide section 2.1.4
	 */
	public static String getReturnTypeFromAsmMethodDescriptor(String methodDescriptor) {
		String returnTypeDescriptor = methodDescriptor.substring(methodDescriptor.indexOf(')') + 1);
		return convertAsmTypeDescriptorToClassName(returnTypeDescriptor);
	}

	/**
	 * Creates a new ASM {@link ClassReader} for <var>pathToClass</var>. Appends '.class' to
	 * pathToClass before attempting to load.
	 * 
	 * @throws RuntimeException if <var>pathToClass</var>+.class cannot be found on the
	 *         classpath
	 * @throws RuntimeException if an IOException occurs when creating the new ClassReader
	 */
	public static ClassReader newAsmClassReader(String pathToClass, ClassLoader classLoader) {
		InputStream is = getClassAsStream(pathToClass, classLoader);
		return newAsmClassReader(is);
	}

	/**
	 * Convenience method that creates and returns a new ASM {@link ClassReader} for the
	 * given InputStream <var>is</var>, closing the InputStream after creating the
	 * ClassReader and rethrowing any IOException thrown during ClassReader instantiation as
	 * an unchecked exception. Logs and ignores any IOException thrown when closing the
	 * InputStream.
	 * 
	 * @param is InputStream that will be provided to the new ClassReader instance.
	 */
	public static ClassReader newAsmClassReader(InputStream is) {
		try {
			return new ClassReader(is);
		} catch (IOException ex) {
			throw new RuntimeException("An unexpected exception occurred while creating ASM ClassReader: " + ex);
		} finally {
			try {
				is.close();
			} catch (IOException ex) {
				log.error("Ignoring exception thrown while closing InputStream", ex);
			}
		}
	}

	/**
	 * Uses the default ClassLoader to load <var>pathToClass</var>. Appends '.class' to
	 * pathToClass before attempting to load.
	 * 
	 * @param pathToClass resource path to class, not including .class suffix. e.g.:
	 *        com/acme/MyClass
	 * 
	 * @return inputStream for <var>pathToClass</var>
	 * 
	 * @throws RuntimeException if <var>pathToClass</var> does not exist
	 */
	public static InputStream getClassAsStream(String pathToClass, ClassLoader classLoader) {
		String classFileName = pathToClass + ClassUtils.CLASS_FILE_SUFFIX;
	
		InputStream is = classLoader.getResourceAsStream(classFileName);
	
		if (is == null)
			throw new RuntimeException(
					new FileNotFoundException("Class file [" + classFileName + "] not found"));
	
		return is;
	}

	/**
	 * Loads the specified class using the default class loader, rethrowing any
	 * {@link ClassNotFoundException} as an unchecked exception.
	 * 
	 * @param <T> type of class to be returned
	 * @param fqClassName fully-qualified class name
	 * 
	 * @return newly loaded class instance
	 * 
	 * @throws IllegalArgumentException if configClassName cannot be loaded.
	 * 
	 * @see #loadClass(String)
	 * @see #loadToolingSafeClass(String)
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> loadRequiredClass(String fqClassName) {
		try {
			return (Class<? extends T>) getDefaultClassLoader().loadClass(fqClassName);
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException(format(
			        "Class [%s] could not be loaded, check your CLASSPATH.", fqClassName), ex);
		}
	}

	/**
	 * Loads the specified class using the default class loader, gracefully handling any
	 * {@link ClassNotFoundException} that may be thrown by issuing a WARN level logging
	 * statement and return null. This functionality is specifically implemented to
	 * accomodate tooling (Spring IDE) concerns, where user-defined types will not be
	 * available to the tooling.
	 * <p>
	 * Because {@link ClassNotFoundException} is compensated for by returning null, callers
	 * must take care to handle the null case appropriately.
	 * <p>
	 * In cases where the WARN logging statement is not desired, use the
	 * {@link #loadClass(String)} method, which returns null but issues no logging
	 * statements.
	 * <p>
	 * This method should only ever return null in the case of a user-defined type be
	 * processed at tooling time. Therefore, tooling may not be able to represent any custom
	 * annotation semantics, but JavaConfig itself will not have any problem loading and
	 * respecting them at actual runtime.
	 * 
	 * @param <T> type of class to be returned
	 * @param fqClassName fully-qualified class name
	 * 
	 * @return newly loaded class, null if class could not be found.
	 * 
	 * @see #loadClass(String)
	 * @see #loadRequiredClass(String)
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> loadToolingSafeClass(String fqClassName, ClassLoader classLoader) {
		try {
			return (Class<? extends T>) classLoader.loadClass(fqClassName);
		} catch (ClassNotFoundException ex) {
			log.warn(format("Unable to load class [%s], likely due to tooling-specific restrictions."
			        + "Attempting to continue, but unexpected errors may occur", fqClassName), ex);
			return null;
		}
	}

	/**
	 * Creates a {@link MutableAnnotation} for {@code annoType}. JDK dynamic proxies are
	 * used, and the returned proxy implements both {@link MutableAnnotation} and annotation
	 * type {@code A}
	 * 
	 * @param <A> annotation type that must be supplied and returned
	 * @param annoType type of annotation to create
	 */
	public static <A extends Annotation> A createMutableAnnotation(Class<A> annoType, ClassLoader classLoader) {
		MutableAnnotationInvocationHandler handler = new MutableAnnotationInvocationHandler(annoType);
		Class<?>[] interfaces = new Class<?>[] { annoType, MutableAnnotation.class };
	
		@SuppressWarnings("unchecked")
		A mutableAnno = (A) Proxy.newProxyInstance(classLoader, interfaces, handler);
		return mutableAnno;
	}

}
