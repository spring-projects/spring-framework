/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type;

import java.lang.reflect.Method;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Sam Brannen
 */
abstract class ClassloadingAssertions {

	private static boolean isClassLoaded(String className) {
		ClassLoader cl = ClassUtils.getDefaultClassLoader();
		Method findLoadedClassMethod = ReflectionUtils.findMethod(cl.getClass(), "findLoadedClass", String.class);
		ReflectionUtils.makeAccessible(findLoadedClassMethod);
		Class<?> loadedClass = (Class<?>) ReflectionUtils.invokeMethod(findLoadedClassMethod, cl, className);
		return loadedClass != null;
	}

	public static void assertClassNotLoaded(String className) {
		assertThat(isClassLoaded(className)).as("Class [" + className + "] should not have been loaded").isFalse();
	}

}
