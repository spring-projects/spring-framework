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

package org.springframework.aop.framework;

import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.ReflectionUtils;

/**
 * Objenesis-based extension of {@link CglibAopProxy} to create proxy instances
 * without invoking the constructor of the class. Used by default.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
class ObjenesisCglibAopProxy extends CglibAopProxy {

	private static final Log logger = LogFactory.getLog(ObjenesisCglibAopProxy.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();


	/**
	 * Create a new ObjenesisCglibAopProxy for the given AOP configuration.
	 * @param config the AOP configuration as AdvisedSupport object
	 */
	public ObjenesisCglibAopProxy(AdvisedSupport config) {
		super(config);
	}


	@Override
	protected Class<?> createProxyClass(Enhancer enhancer) {
		return enhancer.createClass();
	}

	@Override
	protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
		Class<?> proxyClass = enhancer.createClass();
		Object proxyInstance = null;

		if (objenesis.isWorthTrying()) {
			try {
				proxyInstance = objenesis.newInstance(proxyClass, enhancer.getUseCache());
			}
			catch (Throwable ex) {
				logger.debug("Unable to instantiate proxy using Objenesis, falling back to regular construction", ex);
			}
		}

		if (proxyInstance == null) {
			try {
				Constructor<?> ctor = (this.constructorArgs != null ?
						proxyClass.getDeclaredConstructor(this.constructorArgTypes) :
						proxyClass.getDeclaredConstructor());
				ReflectionUtils.makeAccessible(ctor);
				proxyInstance = (this.constructorArgs != null ?
						ctor.newInstance(this.constructorArgs) : ctor.newInstance());
			}
			catch (Throwable ex) {
				throw new AopConfigException("Unable to instantiate proxy", ex);
			}
		}
		/*
		 * Workaround for issue #30985.
		 *
		 * In native image mode, SpringNamingPolicy can generate colliding class names
		 * (all ending with $$SpringCGLIB$$0) when multiple incompatible CGLIB proxies
		 * are needed for the same bean (e.g. @Lazy constructor proxy + AOP class proxy).
		 *
		 * This leads to a ClassCastException when setCallbacks() is called because
		 * the pre-generated proxy class does not match the required callback layout.
		 *
		 * This fallback tries the next numbered proxy class ($$SpringCGLIB$$1,
		 * $$SpringCGLIB$$2, ...) from the classpath until a compatible one is found.
		 *
		 * Note: This is a targeted runtime workaround because changing SpringNamingPolicy
		 * would be a breaking change for many existing components.
		 */
		// === Safe fallback only for SpringNamingPolicy ===
		if (enhancer.getNamingPolicy() == SpringNamingPolicy.INSTANCE) {
			try {
				((Factory) proxyInstance).setCallbacks(callbacks);
				return proxyInstance;
			}
			catch (ClassCastException ex) {
				// Name collision detected ? try next numbered proxy ($$1, $$2, ...)
				String className = proxyClass.getName();
				int lastDoubleDollar = className.lastIndexOf("$$");

				if (lastDoubleDollar == -1) {
					throw ex; // not a Spring CGLIB proxy ? rethrow
				}

				String base = className.substring(0, lastDoubleDollar + 2);
				String numberStr = className.substring(lastDoubleDollar + 2);
				int counter = Integer.parseInt(numberStr);

				// Safety limit to prevent infinite loop
				for (int i = 0; i < 10; i++) {   // max 10 attempts
					counter++;
					String nextName = base + counter;

					try {
						Class<?> nextProxyClass = Class.forName(nextName);
						proxyInstance = (this.constructorArgs != null ?
								nextProxyClass.getDeclaredConstructor(this.constructorArgTypes)
								.newInstance(this.constructorArgs) :
								nextProxyClass.getDeclaredConstructor().newInstance());

						((Factory) proxyInstance).setCallbacks(callbacks);
						logger.debug("Used fallback proxy class: {}");
						return proxyInstance;
					}
					catch (Exception ignored) {
						// Class does not exist or cannot be instantiated ? try next number
					}
				}

				// If we reach here, no suitable proxy was found
				throw new AopConfigException("Could not find a compatible CGLIB proxy for " + className, ex);
			}
		}
		else {
			((Factory) proxyInstance).setCallbacks(callbacks);
		}

		return proxyInstance;
	}

}
