/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.type;

import java.lang.reflect.Method;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

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
		assertFalse("Class [" + className + "] should not have been loaded", isClassLoaded(className));
	}

}
