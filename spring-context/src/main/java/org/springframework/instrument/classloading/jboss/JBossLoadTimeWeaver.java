/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.instrument.classloading.jboss;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link LoadTimeWeaver} implementation for JBoss's instrumentable ClassLoader.
 * Thanks to Ales Justin and Marius Bogoevici for the initial prototype.
 *
 * <p>This weaver supports WildFly 13+.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.0
 */
public class JBossLoadTimeWeaver implements LoadTimeWeaver {

	private static final String DELEGATING_TRANSFORMER_CLASS_NAME =
			"org.jboss.as.server.deployment.module.DelegatingClassFileTransformer";

	private static final String WRAPPER_TRANSFORMER_CLASS_NAME =
			"org.jboss.modules.JLIClassTransformer";


	private final ClassLoader classLoader;

	private final Object delegatingTransformer;

	private final Method addTransformer;


	/**
	 * Create a new instance of the {@link JBossLoadTimeWeaver} class using
	 * the default {@link ClassLoader class loader}.
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	public JBossLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a new instance of the {@link JBossLoadTimeWeaver} class using
	 * the supplied {@link ClassLoader}.
	 * @param classLoader the {@code ClassLoader} to delegate to for weaving
	 */
	public JBossLoadTimeWeaver(@Nullable ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;

		try {
			Field transformer = ReflectionUtils.findField(classLoader.getClass(), "transformer");
			if (transformer == null) {
				throw new IllegalArgumentException("Could not find 'transformer' field on JBoss ClassLoader: " +
						classLoader.getClass().getName());
			}
			transformer.setAccessible(true);

			Object suggestedTransformer = transformer.get(classLoader);
			if (suggestedTransformer.getClass().getName().equals(WRAPPER_TRANSFORMER_CLASS_NAME)) {
				Field wrappedTransformer = ReflectionUtils.findField(suggestedTransformer.getClass(), "transformer");
				if (wrappedTransformer == null) {
					throw new IllegalArgumentException(
							"Could not find 'transformer' field on JBoss JLIClassTransformer: " +
							suggestedTransformer.getClass().getName());
				}
				wrappedTransformer.setAccessible(true);
				suggestedTransformer = wrappedTransformer.get(suggestedTransformer);
			}
			if (!suggestedTransformer.getClass().getName().equals(DELEGATING_TRANSFORMER_CLASS_NAME)) {
				throw new IllegalStateException(
						"Transformer not of the expected type DelegatingClassFileTransformer: " +
						suggestedTransformer.getClass().getName());
			}
			this.delegatingTransformer = suggestedTransformer;

			Method addTransformer = ReflectionUtils.findMethod(this.delegatingTransformer.getClass(),
					"addTransformer", ClassFileTransformer.class);
			if (addTransformer == null) {
				throw new IllegalArgumentException(
						"Could not find 'addTransformer' method on JBoss DelegatingClassFileTransformer: " +
						this.delegatingTransformer.getClass().getName());
			}
			addTransformer.setAccessible(true);
			this.addTransformer = addTransformer;
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not initialize JBoss LoadTimeWeaver", ex);
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		try {
			this.addTransformer.invoke(this.delegatingTransformer, transformer);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not add transformer on JBoss ClassLoader: " + this.classLoader, ex);
		}
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		return new SimpleThrowawayClassLoader(getInstrumentableClassLoader());
	}

}
