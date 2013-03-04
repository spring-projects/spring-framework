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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.core.OverridingClassLoader;

/**
 * Simplistic implementation of an instrumentable {@code ClassLoader}.
 *
 * <p>Usable in tests and standalone environments.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @since 2.0
 */
public class SimpleInstrumentableClassLoader extends OverridingClassLoader {

	private final WeavingTransformer weavingTransformer;


	/**
	 * Create a new {@code SimpleLoadTimeWeaver} for the given
	 * {@code ClassLoader}.
	 * @param parent the {@code ClassLoader} to build a simple
	 * instrumentable {@code ClassLoader} for
	 */
	public SimpleInstrumentableClassLoader(ClassLoader parent) {
		super(parent);
		this.weavingTransformer = new WeavingTransformer(parent);
	}


	/**
	 * Add a {@code ClassFileTransformer} to be applied by this
	 * {@code ClassLoader}.
	 * @param transformer the {@code ClassFileTransformer} to register
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		this.weavingTransformer.addTransformer(transformer);
	}


	@Override
	protected byte[] transformIfNecessary(String name, byte[] bytes) {
		return this.weavingTransformer.transformIfNecessary(name, bytes);
	}

}
