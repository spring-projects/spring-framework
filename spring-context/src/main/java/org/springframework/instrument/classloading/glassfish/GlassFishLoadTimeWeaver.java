/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link LoadTimeWeaver} implementation for GlassFish's
 * {@code org.glassfish.api.deployment.InstrumentableClassLoader InstrumentableClassLoader}.
 *
 * <p>As of Spring 4.0, this weaver supports GlassFish V3 and V4.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0.1
 */
public class GlassFishLoadTimeWeaver implements LoadTimeWeaver {

	private static final String INSTRUMENTABLE_LOADER_CLASS_NAME = "org.glassfish.api.deployment.InstrumentableClassLoader";


	private final ClassLoader classLoader;

	private final Method addTransformerMethod;

	private final Method copyMethod;


	public GlassFishLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	public GlassFishLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");

		Class<?> instrumentableLoaderClass;
		try {
			instrumentableLoaderClass = classLoader.loadClass(INSTRUMENTABLE_LOADER_CLASS_NAME);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException(
					"Could not initialize GlassFishLoadTimeWeaver because GlassFish API classes are not available", ex);
		}
		try {
			this.addTransformerMethod = instrumentableLoaderClass.getMethod("addTransformer", ClassFileTransformer.class);
			this.copyMethod = instrumentableLoaderClass.getMethod("copy");
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize GlassFishLoadTimeWeaver because GlassFish API classes are not available", ex);
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
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		try {
			this.addTransformerMethod.invoke(this.classLoader, transformer);
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("GlassFish addTransformer method threw exception", ex.getCause());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not invoke GlassFish addTransformer method", ex);
		}
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		try {
			return (ClassLoader) this.copyMethod.invoke(this.classLoader);
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("GlassFish copy method threw exception", ex.getCause());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not invoke GlassFish copy method", ex);
		}
	}

}
