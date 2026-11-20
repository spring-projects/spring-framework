/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

import org.jspecify.annotations.Nullable;

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

	static {
		ClassLoader.registerAsParallelCapable();
	}


	private final WeavingTransformer weavingTransformer;


	/**
	 * Create a new SimpleInstrumentableClassLoader for the given ClassLoader.
	 * @param parent the ClassLoader to build an instrumentable ClassLoader for
	 */
	public SimpleInstrumentableClassLoader(@Nullable ClassLoader parent) {
		super(parent);
		this.weavingTransformer = new WeavingTransformer(parent);
	}


	/**
	 * Add a {@link ClassFileTransformer} to be applied by this ClassLoader.
	 * @param transformer the {@link ClassFileTransformer} to register
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		this.weavingTransformer.addTransformer(transformer);
	}


	@Override
	protected byte[] transformIfNecessary(String name, byte[] bytes) {
		return this.weavingTransformer.transformIfNecessary(name, bytes);
	}

}
