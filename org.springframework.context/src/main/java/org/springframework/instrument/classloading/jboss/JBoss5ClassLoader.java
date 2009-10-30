/*
 * Copyright 2002-2009 the original author or authors.
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
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.base.BaseClassLoader;
import org.jboss.util.loading.Translator;

import org.springframework.util.Assert;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;

/**
 * Reflective wrapper around a JBoss5 class loader. Used to encapsulate the classloader-specific methods (discovered and
 * called through reflection) from the load-time weaver.
 *
 * @author Ales Justin
 * @author Marius Bogoevici
 */
public abstract class JBoss5ClassLoader extends ReflectionHelper {

	private final BaseClassLoader classLoader;

	private ClassLoaderPolicy policy;

	@SuppressWarnings("unchecked")
	protected JBoss5ClassLoader(BaseClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;

		try {
			SecurityManager sm = System.getSecurityManager();
			if (sm != null) {
				AccessController.doPrivileged(new InstantiationAction());
			}
			else {
				doInstantiate();
			}
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Could not initialize JBoss ClassLoader because JBoss5 API classes are not available", e);
		}
	}

	/**
	 * Get the policy.
	 *
	 * @return the policy
	 */
	protected ClassLoaderPolicy getPolicy() {
		return policy;
	}

	/**
	 * Do instantiate method, variables.
	 *
	 * @throws Exception for any error
	 */
	private void doInstantiate() throws Exception {
		Method getPolicy = getMethod(BaseClassLoader.class, "getPolicy");
		policy = invokeMethod(getPolicy, classLoader, ClassLoaderPolicy.class);
		fallbackStrategy();
	}

	/**
	 * The fallback strategy.
	 *
	 * @throws Exception for any error
	 */
	protected void fallbackStrategy() throws Exception {
	}

	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "ClassFileTransformer must not be null");
		Translator translator = new ClassFileTransformer2Translator(transformer);
		addTranslator(translator);
	}

	/**
	 * Add the translator.
	 *
	 * @param translator the translator
	 */
	protected abstract void addTranslator(Translator translator);

	public ClassLoader getInternalClassLoader() {
		return classLoader;
	}

	public ClassLoader getThrowawayClassLoader() {
		return new SimpleThrowawayClassLoader(classLoader);
	}

	/** Instantiation action. */
	private class InstantiationAction implements PrivilegedExceptionAction {

		public Object run() throws Exception {
			doInstantiate();
			return null;
		}
	}
}
