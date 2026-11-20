/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@code LoadTimeWeaver} that builds and exposes a
 * {@link SimpleInstrumentableClassLoader}.
 *
 * <p>Mainly intended for testing environments, where it is sufficient to
 * perform all class transformation on a newly created
 * {@code ClassLoader} instance.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see #getInstrumentableClassLoader()
 * @see SimpleInstrumentableClassLoader
 * @see ReflectiveLoadTimeWeaver
 */
public class SimpleLoadTimeWeaver implements LoadTimeWeaver {

	private final SimpleInstrumentableClassLoader classLoader;


	/**
	 * Create a new {@code SimpleLoadTimeWeaver} for the current context
	 * {@code ClassLoader}.
	 * @see SimpleInstrumentableClassLoader
	 */
	public SimpleLoadTimeWeaver() {
		this.classLoader = new SimpleInstrumentableClassLoader(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a new {@code SimpleLoadTimeWeaver} for the given
	 * {@code ClassLoader}.
	 * @param classLoader the {@code ClassLoader} to build a simple
	 * instrumentable {@code ClassLoader} on top of
	 */
	public SimpleLoadTimeWeaver(SimpleInstrumentableClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		this.classLoader.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	/**
	 * This implementation builds a {@link SimpleThrowawayClassLoader}.
	 */
	@Override
	public ClassLoader getThrowawayClassLoader() {
		return new SimpleThrowawayClassLoader(getInstrumentableClassLoader());
	}

}
