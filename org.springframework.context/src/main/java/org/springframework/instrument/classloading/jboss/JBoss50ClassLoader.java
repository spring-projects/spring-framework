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

import java.lang.reflect.Method;

import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.base.BaseClassLoader;
import org.jboss.classloader.spi.base.BaseClassLoaderDomain;
import org.jboss.classloader.spi.base.BaseClassLoaderPolicy;
import org.jboss.classloader.spi.base.BaseClassLoaderSystem;
import org.jboss.util.loading.Translator;

/**
 * Reflective wrapper around a JBoss_5.0.x class loader. Used to encapsulate the classloader-specific methods
 * (discovered and called through reflection) from the load-time weaver.
 *
 * @author Ales Justin
 */
public class JBoss50ClassLoader extends JBoss5ClassLoader {

	private Method addTranslator;

	private ClassLoaderSystem system;

	public JBoss50ClassLoader(BaseClassLoader classLoader) {
		super(classLoader);
	}

	protected void fallbackStrategy() throws Exception {
		try {
			// let's check if we have a patched policy, with translator per policy
			addTranslator = getMethod(BaseClassLoaderPolicy.class, "addTranslator");
		}
		catch (Exception ignored) {
			//log.info("Policy doesn't have addTranslator, falling back to ClassLoaderSystem.");

			Method getClassLoaderDomain = getMethod(BaseClassLoaderPolicy.class, "getClassLoaderDomain");
			BaseClassLoaderDomain domain = invokeMethod(getClassLoaderDomain, getPolicy(), BaseClassLoaderDomain.class);
			Method getClassLoaderSystem = getMethod(BaseClassLoaderDomain.class, "getClassLoaderSystem");
			BaseClassLoaderSystem system = invokeMethod(getClassLoaderSystem, domain, BaseClassLoaderSystem.class);
			if (system instanceof ClassLoaderSystem) {
				this.system = ClassLoaderSystem.class.cast(system);
			}
			else {
				throw new IllegalArgumentException(
						"ClassLoaderSystem must be instance of [" + ClassLoaderSystem.class.getName() + "]");
			}
		}
	}

	protected void addTranslator(Translator translator) {
		if (addTranslator != null) {
			try {
				addTranslator.invoke(translator);
			}
			catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
		else {
			system.setTranslator(translator);
		}
	}
}
