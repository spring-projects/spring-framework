/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.context.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * Special variant of an overriding ClassLoader, used for temporary type
 * matching in {@link AbstractApplicationContext}. Redefines classes from
 * a cached byte array for every {@code loadClass} call in order to
 * pick up recently loaded types in the parent ClassLoader.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see AbstractApplicationContext
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setTempClassLoader
 */
class ContextTypeMatchClassLoader extends DecoratingClassLoader implements SmartClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}


	private static Method findLoadedClassMethod;

	static {
		try {
			findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Invalid [java.lang.ClassLoader] class: no 'findLoadedClass' method defined!");
		}
	}


	/** Cache for byte array per class name */
	private final Map<String, byte[]> bytesCache = new ConcurrentHashMap<>(256);


	public ContextTypeMatchClassLoader(@Nullable ClassLoader parent) {
		super(parent);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return new ContextOverridingClassLoader(getParent()).loadClass(name);
	}

	@Override
	public boolean isClassReloadable(Class<?> clazz) {
		return (clazz.getClassLoader() instanceof ContextOverridingClassLoader);
	}


	/**
	 * ClassLoader to be created for each loaded class.
	 * Caches class file content but redefines class for each call.
	 */
	private class ContextOverridingClassLoader extends OverridingClassLoader {

		public ContextOverridingClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			if (isExcluded(className) || ContextTypeMatchClassLoader.this.isExcluded(className)) {
				return false;
			}
			ReflectionUtils.makeAccessible(findLoadedClassMethod);
			ClassLoader parent = getParent();
			while (parent != null) {
				if (ReflectionUtils.invokeMethod(findLoadedClassMethod, parent, className) != null) {
					return false;
				}
				parent = parent.getParent();
			}
			return true;
		}

		@Override
		protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
			byte[] bytes = bytesCache.get(name);
			if (bytes == null) {
				bytes = loadBytesForClass(name);
				if (bytes != null) {
					bytesCache.put(name, bytes);
				}
				else {
					return null;
				}
			}
			return defineClass(name, bytes, 0, bytes.length);
		}
	}

}
