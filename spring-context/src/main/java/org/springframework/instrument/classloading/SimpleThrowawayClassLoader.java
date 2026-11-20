/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.instrument.classloading;

import org.jspecify.annotations.Nullable;

import org.springframework.core.OverridingClassLoader;

/**
 * ClassLoader that can be used to load classes without bringing them
 * into the parent loader. Intended to support JPA "temp class loader"
 * requirement, but not JPA-specific.
 *
 * @author Rod Johnson
 * @since 2.0
 */
public class SimpleThrowawayClassLoader extends OverridingClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}


	/**
	 * Create a new SimpleThrowawayClassLoader for the given ClassLoader.
	 * @param parent the ClassLoader to build a throwaway ClassLoader for
	 */
	public SimpleThrowawayClassLoader(@Nullable ClassLoader parent) {
		super(parent);
	}

}
