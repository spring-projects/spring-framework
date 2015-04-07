/*
 * Copyright 2002-2013 the original author or authors.
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.util.ReflectionUtils;

/**
 * Reflective wrapper around a JBoss 6 class loader methods
 * (discovered and called through reflection) for load-time weaving.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
class JBossMCAdapter implements JBossClassLoaderAdapter {

	private static final String LOADER_NAME = "org.jboss.classloader.spi.base.BaseClassLoader";

	private static final String TRANSLATOR_NAME = "org.jboss.util.loading.Translator";


	private final ClassLoader classLoader;

	private final Object target;

	private final Class<?> translatorClass;

	private final Method addTranslator;


	public JBossMCAdapter(ClassLoader classLoader) {
		try {
			// Resolve BaseClassLoader.class
			Class<?> clazzLoaderType = classLoader.loadClass(LOADER_NAME);

			ClassLoader clazzLoader = null;
			// Walk the hierarchy to detect the instrumentation aware ClassLoader
			for (ClassLoader cl = classLoader; cl != null && clazzLoader == null; cl = cl.getParent()) {
				if (clazzLoaderType.isInstance(cl)) {
					clazzLoader = cl;
				}
			}

			if (clazzLoader == null) {
				throw new IllegalArgumentException(classLoader + " and its parents are not suitable ClassLoaders: " +
						"A [" + LOADER_NAME + "] implementation is required.");
			}

			this.classLoader = clazzLoader;
			// Use the ClassLoader that loaded the ClassLoader to load the types for reflection purposes
			classLoader = clazzLoader.getClass().getClassLoader();

			// BaseClassLoader#getPolicy
			Method method = clazzLoaderType.getDeclaredMethod("getPolicy");
			ReflectionUtils.makeAccessible(method);
			this.target = method.invoke(this.classLoader);

			// Check existence of BaseClassLoaderPolicy#addTranslator(Translator)
			this.translatorClass = classLoader.loadClass(TRANSLATOR_NAME);
			this.addTranslator = this.target.getClass().getMethod("addTranslator", this.translatorClass);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize JBoss LoadTimeWeaver because the JBoss 6 API classes are not available", ex);
		}
	}

	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		InvocationHandler adapter = new JBossMCTranslatorAdapter(transformer);
		Object adapterInstance = Proxy.newProxyInstance(this.translatorClass.getClassLoader(),
				new Class<?>[] {this.translatorClass}, adapter);
		try {
			this.addTranslator.invoke(this.target, adapterInstance);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not add transformer on JBoss 6 ClassLoader " + this.classLoader, ex);
		}
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

}
