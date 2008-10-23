/*
 * Copyright 2002-2007 the original author or authors.
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

import com.sun.enterprise.loader.InstrumentableClassLoader;

import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link LoadTimeWeaver} implementation for GlassFish's
 * {@link InstrumentableClassLoader}.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see com.sun.enterprise.loader.InstrumentableClassLoader
 */
public class GlassFishLoadTimeWeaver implements LoadTimeWeaver {

	private final InstrumentableClassLoader classLoader;


	/**
	 * Create a new instance of the <code>GlassFishLoadTimeWeaver</code> class
	 * using the default {@link ClassLoader}.
	 * @see #GlassFishLoadTimeWeaver(ClassLoader)
	 */
	public GlassFishLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a new instance of the <code>GlassFishLoadTimeWeaver</code> class.
	 * @param classLoader the specific {@link ClassLoader} to use; must not be <code>null</code>
	 * @throws IllegalArgumentException if the supplied <code>classLoader</code> is <code>null</code>;
	 * or if the supplied <code>classLoader</code> is not an {@link InstrumentableClassLoader}
	 */
	public GlassFishLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		InstrumentableClassLoader icl = determineClassLoader(classLoader);
		if (icl == null) {
			throw new IllegalArgumentException(classLoader + " and its parents are not suitable ClassLoaders: " +
					"An [" + InstrumentableClassLoader.class.getName() + "] implementation is required.");
		}
		this.classLoader = icl;
	}

	/**
	 * Determine the GlassFish {@link InstrumentableClassLoader} for the given
	 * {@link ClassLoader}.
	 * @param classLoader the <code>ClassLoader</code> to check
	 * @return the <code>InstrumentableClassLoader</code>, or <code>null</code> if none found
	 */
	protected InstrumentableClassLoader determineClassLoader(ClassLoader classLoader) {
		// Detect transformation-aware ClassLoader by traversing the hierarchy
		// (as in GlassFish, Spring can be loaded by the WebappClassLoader).
		for (ClassLoader cl = classLoader; cl != null; cl = cl.getParent()) {
			if (cl instanceof InstrumentableClassLoader) {
				return (InstrumentableClassLoader) cl;
			}
		}
		return null;
	}


	public void addTransformer(ClassFileTransformer transformer) {
		this.classLoader.addTransformer(new ClassTransformerAdapter(transformer));
	}

	public ClassLoader getInstrumentableClassLoader() {
		return (ClassLoader) this.classLoader;
	}

	public ClassLoader getThrowawayClassLoader() {
		return this.classLoader.copy();
	}

}
