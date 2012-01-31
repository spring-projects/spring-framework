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

package org.springframework.instrument.classloading.glassfish;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link LoadTimeWeaver} implementation for GlassFish's {@link InstrumentableClassLoader}.
 * 
 * <p>As of Spring 3.0, GlassFish V3 is supported as well.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0.1
 */
public class GlassFishLoadTimeWeaver implements LoadTimeWeaver {

	private final GlassFishClassLoaderAdapter classLoader;


	/**
	 * Creates a new instance of the <code>GlassFishLoadTimeWeaver</code> class
	 * using the default {@link ClassLoader}.
	 * @see #GlassFishLoadTimeWeaver(ClassLoader)
	 */
	public GlassFishLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Creates a new instance of the <code>GlassFishLoadTimeWeaver</code> class.
	 * @param classLoader the specific {@link ClassLoader} to use; must not be <code>null</code>
	 * @throws IllegalArgumentException if the supplied <code>classLoader</code> is <code>null</code>;
	 * or if the supplied <code>classLoader</code> is not an {@link InstrumentableClassLoader}
	 */
	public GlassFishLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = new GlassFishClassLoaderAdapter(classLoader);
	}


	public void addTransformer(ClassFileTransformer transformer) {
		this.classLoader.addTransformer(transformer);
	}

	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader.getClassLoader();
	}

	public ClassLoader getThrowawayClassLoader() {
		return this.classLoader.getThrowawayClassLoader();
	}

}
