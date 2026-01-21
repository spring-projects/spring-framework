/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.aot;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

/**
 * Unit tests for {@link AotContextLoader}.
 *
 * @author Sam Brannen
 * @since 6.2.4
 */
class AotContextLoaderTests {

	/**
	 * Verifies that a legacy {@link AotContextLoader} which only overrides
	 * {@link AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
	 * is still supported.
	 */
	@Test  // gh-34513
	@SuppressWarnings("removal")
	void legacyAotContextLoader() throws Exception {
		// Prerequisites
		assertDeclaringClasses(LegacyAotContextLoader.class, LegacyAotContextLoader.class, AotContextLoader.class);

		AotContextLoader loader = spy(new LegacyAotContextLoader());
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(), null, null, null, loader);

		loader.loadContextForAotProcessing(mergedConfig, new RuntimeHints());

		then(loader).should().loadContextForAotProcessing(mergedConfig);
	}

	/**
	 * Verifies that a modern {@link AotContextLoader} which only overrides
	 * {@link AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration, RuntimeHints)
	 * is supported.
	 */
	@Test  // gh-34513
	@SuppressWarnings("removal")
	void runtimeHintsAwareAotContextLoader() throws Exception {
		// Prerequisites
		assertDeclaringClasses(RuntimeHintsAwareAotContextLoader.class, AotContextLoader.class, RuntimeHintsAwareAotContextLoader.class);

		AotContextLoader loader = spy(new RuntimeHintsAwareAotContextLoader());
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(), null, null, null, loader);

		loader.loadContextForAotProcessing(mergedConfig, new RuntimeHints());

		then(loader).should(never()).loadContextForAotProcessing(mergedConfig);
	}


	private static void assertDeclaringClasses(Class<? extends AotContextLoader> loaderClass,
			Class<?> declaringClassForLegacyMethod, Class<?> declaringClassForNewMethod) throws Exception {

		Method legacyMethod = loaderClass.getMethod("loadContextForAotProcessing", MergedContextConfiguration.class);
		Method newMethod = loaderClass.getMethod("loadContextForAotProcessing", MergedContextConfiguration.class, RuntimeHints.class);

		assertThat(legacyMethod.getDeclaringClass()).isEqualTo(declaringClassForLegacyMethod);
		assertThat(newMethod.getDeclaringClass()).isEqualTo(declaringClassForNewMethod);
	}


	private static class LegacyAotContextLoader extends AbstractAotContextLoader {

		@Override
		@SuppressWarnings("removal")
		public GenericApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig) {
			return loadContext(mergedConfig);
		}
	}

	private static class RuntimeHintsAwareAotContextLoader extends AbstractAotContextLoader {

		@Override
		public GenericApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig,
				RuntimeHints runtimeHints) {

			return loadContext(mergedConfig);
		}
	}

}
