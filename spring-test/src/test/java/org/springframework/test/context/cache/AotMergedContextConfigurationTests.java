/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.cache;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.DelegatingSmartContextLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AotMergedContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class AotMergedContextConfigurationTests {

	private final CacheAwareContextLoaderDelegate delegate =
			new DefaultCacheAwareContextLoaderDelegate(mock());

	private final ContextLoader contextLoader = new DelegatingSmartContextLoader();

	private final MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(), null, null,
		Set.of(DemoApplicationContextInitializer.class), null, contextLoader);

	private final AotMergedContextConfiguration aotMergedConfig1 = new AotMergedContextConfiguration(getClass(),
		DemoApplicationContextInitializer.class, mergedConfig, delegate);

	private final AotMergedContextConfiguration aotMergedConfig2 = new AotMergedContextConfiguration(getClass(),
		DemoApplicationContextInitializer.class, mergedConfig, delegate);


	@Test
	void equals() {
		assertThat(aotMergedConfig1).isEqualTo(aotMergedConfig1);
		assertThat(aotMergedConfig1).isEqualTo(aotMergedConfig2);

		assertThat(mergedConfig).isNotEqualTo(aotMergedConfig1);
		assertThat(aotMergedConfig1).isNotEqualTo(mergedConfig);
	}

	@Test
	void hashCodeBehavior() {
		assertThat(aotMergedConfig1).hasSameHashCodeAs(aotMergedConfig2);

		assertThat(aotMergedConfig1).doesNotHaveSameHashCodeAs(mergedConfig);
	}


	static class DemoApplicationContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
		@Override
		public void initialize(GenericApplicationContext applicationContext) {
		}
	}

}
