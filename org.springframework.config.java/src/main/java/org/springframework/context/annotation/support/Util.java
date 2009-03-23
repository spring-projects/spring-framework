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
import java.io.InputStream;
import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import sun.security.x509.Extension;


/**
 * Misc utils
 * 
 * @author Chris Beams
 */
class Util {

	private static final Log log = LogFactory.getLog(Util.class);

	private Util() {
	}

	/**
	 * Returns instance of type T by invoking its default or no-arg constructor.
	 * <p>
	 * Any reflection-related issues are re-thrown as unchecked.
	 */
	public static <T> T getInstance(Class<? extends T> clazz) {
		try {
			Constructor<? extends T> noArgCtor = clazz.getDeclaredConstructor();
			ReflectionUtils.makeAccessible(noArgCtor);
			return noArgCtor.newInstance();
		} catch (Exception ex) {
			ReflectionUtils.handleReflectionException(ex);
			throw new IllegalStateException(format("Unexpected reflection exception - %s: %s", ex.getClass()
			        .getName(), ex.getMessage()));
		}
	}

	/**
	 * Loads the specified class using the default class loader, gracefully handling any
	 * {@link ClassNotFoundException} that may be thrown. This functionality is specifically
	 * implemented to accomodate tooling (Spring IDE) concerns, where user-defined types
	 * will not be
	 * 
	 * @param <T> type of class to be returned
	 * @param fqClassName fully-qualified class name
	 * 
	 * @return newly loaded class instance, null if class could not be found
	 * 
	 * @see #loadRequiredClass(String)
	 * @see #loadToolingSafeClass(String)
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> loadClass(String fqClassName) {
		try {
			return (Class<? extends T>) ClassUtils.getDefaultClassLoader().loadClass(fqClassName);
		} catch (ClassNotFoundException ex) {
			return null;
		}
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
	 * ASM class reading is used throughout JavaConfig, but there are certain cases where
	 * classloading cannot be avoided - specifically in cases where users define their own
	 * {@link Extension} annotations. This method should therefore be
	 * used sparingly but consistently where required.
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

}
