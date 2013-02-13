/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Reflective wrapper around a JBoss 5 and 6 class loader methods (discovered and called
 * through reflection) for load time weaving.
 *
 * @author Costin Leau
 * @since 3.1
 */
class JBossMCAdapter implements JBossClassLoaderAdapter {

	private static final String TRANSLATOR_NAME = "org.jboss.util.loading.Translator";
	private static final String POLICY_NAME = "org.jboss.classloader.spi.base.BaseClassLoaderPolicy";
	private static final String DOMAIN_NAME = "org.jboss.classloader.spi.base.BaseClassLoaderDomain";
	private static final String DEDICATED_SYSTEM = "org.jboss.classloader.spi.ClassLoaderSystem";
	private static final String LOADER_NAME = "org.jboss.classloader.spi.base.BaseClassLoader";
	private static final String GET_POLICY = "getPolicy";
	private static final String GET_DOMAIN = "getClassLoaderDomain";
	private static final String GET_SYSTEM = "getClassLoaderSystem";

	// available since JBoss AS 5.1.0 / MC 2.0.6 (allows multiple transformers to be added)
	private static final String ADD_TRANSLATOR_NAME = "addTranslator";
	// available since JBoss AS 5.0.0 / MC 2.0.1 (allows only one transformer to be added)
	private static final String SET_TRANSLATOR_NAME = "setTranslator";

	private final ClassLoader classLoader;
	private final Class<?> translatorClass;

	private final Method addTranslator;
	private final Object target;

	JBossMCAdapter(ClassLoader classLoader) {
		Class<?> clazzLoaderType = null;
		try {
			// resolve BaseClassLoader.class
			clazzLoaderType = classLoader.loadClass(LOADER_NAME);

			ClassLoader clazzLoader = null;
			// walk the hierarchy to detect the instrumentation aware classloader
			for (ClassLoader cl = classLoader; cl != null && clazzLoader == null; cl = cl.getParent()) {
				if (clazzLoaderType.isInstance(cl)) {
					clazzLoader = cl;
				}
			}

			if (clazzLoader == null) {
				throw new IllegalArgumentException(classLoader + " and its parents are not suitable ClassLoaders: "
						+ "A [" + LOADER_NAME + "] implementation is required.");
			}

			this.classLoader = clazzLoader;
			// use the classloader that loaded the classloader to load
			// the types for reflection purposes
			classLoader = clazzLoader.getClass().getClassLoader();

			// BaseClassLoader#getPolicy
			Method method = clazzLoaderType.getDeclaredMethod(GET_POLICY);
			ReflectionUtils.makeAccessible(method);
			Object policy = method.invoke(this.classLoader);

			Object addTarget = null;
			Method addMethod = null;

			// try the 5.1.x hooks
			// check existence of BaseClassLoaderPolicy#addTranslator(Translator)
			this.translatorClass = classLoader.loadClass(TRANSLATOR_NAME);
			Class<?> clazz = classLoader.loadClass(POLICY_NAME);
			try {
				addMethod = clazz.getDeclaredMethod(ADD_TRANSLATOR_NAME, translatorClass);
				addTarget = policy;
			} catch (NoSuchMethodException ex) {
			}

			// fall back to 5.0.x method
			if (addMethod == null) {

				// BaseClassLoaderPolicy#getClassLoaderDomain
				method = clazz.getDeclaredMethod(GET_DOMAIN);
				ReflectionUtils.makeAccessible(method);
				Object domain = method.invoke(policy);

				// BaseClassLoaderDomain#getClassLoaderSystem
				clazz = classLoader.loadClass(DOMAIN_NAME);
				method = clazz.getDeclaredMethod(GET_SYSTEM);
				ReflectionUtils.makeAccessible(method);
				Object system = method.invoke(domain);

				// resolve ClassLoaderSystem
				clazz = classLoader.loadClass(DEDICATED_SYSTEM);
				Assert.isInstanceOf(clazz, system, "JBoss LoadTimeWeaver requires JBoss loader system of type "
						+ clazz.getName() + " on JBoss 5.0.x");

				// ClassLoaderSystem#setTranslator
				addMethod = clazz.getDeclaredMethod(SET_TRANSLATOR_NAME, translatorClass);
				addTarget = system;
			}

			this.addTranslator = addMethod;
			this.target = addTarget;

		} catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize JBoss LoadTimeWeaver because the JBoss 5 API classes are not available", ex);
		}
	}

	public void addTransformer(ClassFileTransformer transformer) {
		InvocationHandler adapter = new JBossMCTranslatorAdapter(transformer);
		Object adapterInstance = Proxy.newProxyInstance(this.translatorClass.getClassLoader(),
				new Class[] { this.translatorClass }, adapter);

		try {
			addTranslator.invoke(target, adapterInstance);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not add transformer on JBoss 5/6 classloader " + classLoader, ex);
		}
	}

	public ClassLoader getInstrumentableClassLoader() {
		return classLoader;
	}
}