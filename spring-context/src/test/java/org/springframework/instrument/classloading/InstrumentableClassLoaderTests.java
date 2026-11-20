/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.instrument.classloading;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class InstrumentableClassLoaderTests {

	@Test
	void defaultLoadTimeWeaver() {
		ClassLoader loader = new SimpleInstrumentableClassLoader(ClassUtils.getDefaultClassLoader());
		ReflectiveLoadTimeWeaver handler = new ReflectiveLoadTimeWeaver(loader);
		assertThat(handler.getInstrumentableClassLoader()).isSameAs(loader);
	}

}
