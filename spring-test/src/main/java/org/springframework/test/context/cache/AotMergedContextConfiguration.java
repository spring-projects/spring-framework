/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.cache;

import java.util.Collections;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.style.DefaultToStringStyler;
import org.springframework.core.style.SimpleValueStyler;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link MergedContextConfiguration} implementation based on an AOT-generated
 * {@link ApplicationContextInitializer} that is used to load an AOT-optimized
 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
 *
 * <p>An {@code ApplicationContext} should not be loaded using the metadata in
 * this {@code AotMergedContextConfiguration}. Rather the metadata from the
 * {@linkplain #getOriginal() original} {@code MergedContextConfiguration} must
 * be used.
 *
 * @author Sam Brannen
 * @since 6.0
 */
final class AotMergedContextConfiguration extends MergedContextConfiguration {

	private static final long serialVersionUID = 1963364911008547843L;

	private final Class<? extends ApplicationContextInitializer<?>> contextInitializerClass;

	private final MergedContextConfiguration original;


	AotMergedContextConfiguration(Class<?> testClass,
			Class<? extends ApplicationContextInitializer<?>> contextInitializerClass,
			MergedContextConfiguration original,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		super(testClass, null, null, Collections.singleton(contextInitializerClass), null,
				original.getContextLoader(), cacheAwareContextLoaderDelegate, original.getParent());
		this.contextInitializerClass = contextInitializerClass;
		this.original = original;
	}


	/**
	 * Get the original {@link MergedContextConfiguration} that this
	 * {@code AotMergedContextConfiguration} was created for.
	 */
	MergedContextConfiguration getOriginal() {
		return this.original;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		return ((other instanceof AotMergedContextConfiguration that) &&
				this.contextInitializerClass.equals(that.contextInitializerClass));
	}

	@Override
	public int hashCode() {
		return this.contextInitializerClass.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringCreator(this, new DefaultToStringStyler(new SimpleValueStyler()))
				.append("testClass", getTestClass())
				.append("contextInitializerClass", this.contextInitializerClass)
				.append("original", this.original)
				.toString();
	}

}
