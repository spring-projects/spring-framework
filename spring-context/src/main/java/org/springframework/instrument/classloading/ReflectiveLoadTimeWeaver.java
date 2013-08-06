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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link LoadTimeWeaver} which uses reflection to delegate to an underlying ClassLoader
 * with well-known transformation hooks. The underlying ClassLoader is expected to
 * support the following weaving methods (as defined in the {@link LoadTimeWeaver}
 * interface):
 * <ul>
 * <li>{@code public void addTransformer(java.lang.instrument.ClassFileTransformer)}:
 * for registering the given ClassFileTransformer on this ClassLoader
 * <li>{@code public ClassLoader getThrowawayClassLoader()}:
 * for obtaining a throwaway class loader for this ClassLoader (optional;
 * ReflectiveLoadTimeWeaver will fall back to a SimpleThrowawayClassLoader if
 * that method isn't available)
 * </ul>
 *
 * <p>Please note that the above methods <i>must</i> reside in a class that is
 * publicly accessible, although the class itself does not have to be visible
 * to the application's class loader.
 *
 * <p>The reflective nature of this LoadTimeWeaver is particularly useful when the
 * underlying ClassLoader implementation is loaded in a different class loader itself
 * (such as the application server's class loader which is not visible to the
 * web application). There is no direct API dependency between this LoadTimeWeaver
 * adapter and the underlying ClassLoader, just a 'loose' method contract.
 *
 * <p>This is the LoadTimeWeaver to use in combination with Spring's
 * {@link org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader}
 * for Tomcat 5.0+ as well as with the Resin application server version 3.1+.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 * @see #addTransformer(java.lang.instrument.ClassFileTransformer)
 * @see #getThrowawayClassLoader()
 * @see SimpleThrowawayClassLoader
 * @see org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader
 */
public class ReflectiveLoadTimeWeaver implements LoadTimeWeaver {

	private static final String ADD_TRANSFORMER_METHOD_NAME = "addTransformer";

	private static final String GET_THROWAWAY_CLASS_LOADER_METHOD_NAME = "getThrowawayClassLoader";

	private static final Log logger = LogFactory.getLog(ReflectiveLoadTimeWeaver.class);


	private final ClassLoader classLoader;

	private final Method addTransformerMethod;

	private final Method getThrowawayClassLoaderMethod;


	/**
	 * Create a new ReflectiveLoadTimeWeaver for the current context class
	 * loader, <i>which needs to support the required weaving methods</i>.
	 */
	public ReflectiveLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a new SimpleLoadTimeWeaver for the given class loader.
	 * @param classLoader the {@code ClassLoader} to delegate to for
	 * weaving (<i>must</i> support the required weaving methods).
	 * @throws IllegalStateException if the supplied {@code ClassLoader}
	 * does not support the required weaving methods
	 */
	public ReflectiveLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;
		this.addTransformerMethod = ClassUtils.getMethodIfAvailable(
				this.classLoader.getClass(), ADD_TRANSFORMER_METHOD_NAME,
				new Class[] {ClassFileTransformer.class});
		if (this.addTransformerMethod == null) {
			throw new IllegalStateException(
					"ClassLoader [" + classLoader.getClass().getName() + "] does NOT provide an " +
					"'addTransformer(ClassFileTransformer)' method.");
		}
		this.getThrowawayClassLoaderMethod = ClassUtils.getMethodIfAvailable(
				this.classLoader.getClass(), GET_THROWAWAY_CLASS_LOADER_METHOD_NAME, new Class[0]);
		// getThrowawayClassLoader method is optional
		if (this.getThrowawayClassLoaderMethod == null) {
			if (logger.isInfoEnabled()) {
				logger.info("The ClassLoader [" + classLoader.getClass().getName() + "] does NOT provide a " +
						"'getThrowawayClassLoader()' method; SimpleThrowawayClassLoader will be used instead.");
			}
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		ReflectionUtils.invokeMethod(this.addTransformerMethod, this.classLoader, transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		if (this.getThrowawayClassLoaderMethod != null) {
			return (ClassLoader) ReflectionUtils.invokeMethod(this.getThrowawayClassLoaderMethod, this.classLoader);
		}
		else {
			return new SimpleThrowawayClassLoader(this.classLoader);
		}
	}

}
