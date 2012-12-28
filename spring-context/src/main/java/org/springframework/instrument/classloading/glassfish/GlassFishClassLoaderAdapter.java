/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.instrument.classloading.glassfish;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflective wrapper around the GlassFish class loader. Used to
 * encapsulate the classloader-specific methods (discovered and
 * called through reflection) from the load-time weaver.
 *
 * <p>Supports GlassFish V1, V2 and V3 (currently in beta).
 *
 * @author Costin Leau
 * @since 3.0
 */
class GlassFishClassLoaderAdapter {

	static final String INSTRUMENTABLE_CLASSLOADER_GLASSFISH_V2 = "com.sun.enterprise.loader.InstrumentableClassLoader";

	static final String INSTRUMENTABLE_CLASSLOADER_GLASSFISH_V3 = "org.glassfish.api.deployment.InstrumentableClassLoader";

	private static final String CLASS_TRANSFORMER = "javax.persistence.spi.ClassTransformer";


	private final ClassLoader classLoader;

	private final Method addTransformer;

	private final Method copy;

	private final boolean glassFishV3;


	public GlassFishClassLoaderAdapter(ClassLoader classLoader) {
		Class<?> instrumentableLoaderClass;
		boolean glassV3 = false;
		try {
			// try the V1/V2 API first
			instrumentableLoaderClass = classLoader.loadClass(INSTRUMENTABLE_CLASSLOADER_GLASSFISH_V2);
		}
		catch (ClassNotFoundException ex) {
			// fall back to V3
			try {
				instrumentableLoaderClass = classLoader.loadClass(INSTRUMENTABLE_CLASSLOADER_GLASSFISH_V3);
				glassV3 = true;
			}
			catch (ClassNotFoundException cnfe) {
				throw new IllegalStateException("Could not initialize GlassFish LoadTimeWeaver because " +
						"GlassFish (V1, V2 or V3) API classes are not available", ex);
			}
		}
		try {
			Class<?> classTransformerClass =
					(glassV3 ? ClassFileTransformer.class : classLoader.loadClass(CLASS_TRANSFORMER));

			this.addTransformer = instrumentableLoaderClass.getMethod("addTransformer", classTransformerClass);
			this.copy = instrumentableLoaderClass.getMethod("copy");
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize GlassFish LoadTimeWeaver because GlassFish API classes are not available", ex);
		}

		ClassLoader clazzLoader = null;
		// Detect transformation-aware ClassLoader by traversing the hierarchy
		// (as in GlassFish, Spring can be loaded by the WebappClassLoader).
		for (ClassLoader cl = classLoader; cl != null && clazzLoader == null; cl = cl.getParent()) {
			if (instrumentableLoaderClass.isInstance(cl)) {
				clazzLoader = cl;
			}
		}

		if (clazzLoader == null) {
			throw new IllegalArgumentException(classLoader + " and its parents are not suitable ClassLoaders: A [" +
					instrumentableLoaderClass.getName() + "] implementation is required.");
		}

		this.classLoader = clazzLoader;
		this.glassFishV3 = glassV3;
	}

	public void addTransformer(ClassFileTransformer transformer) {
		try {
			this.addTransformer.invoke(this.classLoader,
					(this.glassFishV3 ? transformer : new ClassTransformerAdapter(transformer)));
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("GlassFish addTransformer method threw exception ", ex.getCause());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not invoke GlassFish addTransformer method", ex);
		}
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	public ClassLoader getThrowawayClassLoader() {
		try {
			return (ClassLoader) this.copy.invoke(this.classLoader);
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("GlassFish copy method threw exception ", ex.getCause());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not invoke GlassFish copy method", ex);
		}
	}

}
