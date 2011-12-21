/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.instrument.classloading.jboss;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * JBoss 7 adapter.
 *
 * @author Costin Leau
 * @since 3.1
 */
class JBossModulesAdapter implements JBossClassLoaderAdapter {

	private static final String TRANSFORMER_FIELD_NAME = "transformer";
	private static final String TRANSFORMER_ADD_METHOD_NAME = "addTransformer";
	private static final String DELEGATING_TRANSFORMER_CLASS_NAME = "org.jboss.as.server.deployment.module.DelegatingClassFileTransformer";
	private final ClassLoader classLoader;
	private final Method addTransformer;
	private final Object delegatingTransformer;

	public JBossModulesAdapter(ClassLoader loader) {
		this.classLoader = loader;

		try {
			Field transformers = ReflectionUtils.findField(classLoader.getClass(), TRANSFORMER_FIELD_NAME);
			transformers.setAccessible(true);

			delegatingTransformer = transformers.get(classLoader);

			Assert.state(delegatingTransformer.getClass().getName().equals(DELEGATING_TRANSFORMER_CLASS_NAME),
					"Transformer not of the expected type: " + delegatingTransformer.getClass().getName());
			addTransformer = ReflectionUtils.findMethod(delegatingTransformer.getClass(), TRANSFORMER_ADD_METHOD_NAME,
					ClassFileTransformer.class);
			addTransformer.setAccessible(true);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not initialize JBoss 7 LoadTimeWeaver", ex);
		}
	}

	public void addTransformer(ClassFileTransformer transformer) {
		try {
			addTransformer.invoke(delegatingTransformer, transformer);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not add transformer on JBoss 7 classloader " + classLoader, ex);
		}
	}

	public ClassLoader getInstrumentableClassLoader() {
		return classLoader;
	}
}