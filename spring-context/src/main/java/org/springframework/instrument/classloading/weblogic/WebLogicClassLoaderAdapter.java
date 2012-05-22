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

package org.springframework.instrument.classloading.weblogic;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.util.Assert;

/**
 * Reflective wrapper around a WebLogic 10 class loader. Used to
 * encapsulate the classloader-specific methods (discovered and
 * called through reflection) from the load-time weaver.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.5
 */
class WebLogicClassLoaderAdapter {

	private static final String GENERIC_CLASS_LOADER_NAME = "weblogic.utils.classloaders.GenericClassLoader";

	private static final String CLASS_PRE_PROCESSOR_NAME = "weblogic.utils.classloaders.ClassPreProcessor";


	private final ClassLoader classLoader;

	private final Class<?> wlPreProcessorClass;

	private final Method addPreProcessorMethod;

	private final Method getClassFinderMethod;

	private final Method getParentMethod;

	private final Constructor<?> wlGenericClassLoaderConstructor;


	public WebLogicClassLoaderAdapter(ClassLoader classLoader) {
		Class<?> wlGenericClassLoaderClass = null;
		try {
			wlGenericClassLoaderClass = classLoader.loadClass(GENERIC_CLASS_LOADER_NAME);
			this.wlPreProcessorClass = classLoader.loadClass(CLASS_PRE_PROCESSOR_NAME);
			this.addPreProcessorMethod = classLoader.getClass().getMethod(
					"addInstanceClassPreProcessor", this.wlPreProcessorClass);
			this.getClassFinderMethod = classLoader.getClass().getMethod("getClassFinder");
			this.getParentMethod = classLoader.getClass().getMethod("getParent");
			this.wlGenericClassLoaderConstructor = wlGenericClassLoaderClass.getConstructor(
					this.getClassFinderMethod.getReturnType(), ClassLoader.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize WebLogic LoadTimeWeaver because WebLogic 10 API classes are not available", ex);
		}
		Assert.isInstanceOf(wlGenericClassLoaderClass, classLoader,
				"ClassLoader must be instance of [" + wlGenericClassLoaderClass.getName() + "]");
		this.classLoader = classLoader;
	}


	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "ClassFileTransformer must not be null");
		try {
			InvocationHandler adapter = new WebLogicClassPreProcessorAdapter(transformer, this.classLoader);
			Object adapterInstance = Proxy.newProxyInstance(this.wlPreProcessorClass.getClassLoader(),
					new Class[] {this.wlPreProcessorClass}, adapter);
			this.addPreProcessorMethod.invoke(this.classLoader, adapterInstance);
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("WebLogic addInstanceClassPreProcessor method threw exception", ex.getCause());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not invoke WebLogic addInstanceClassPreProcessor method", ex);
		}
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	public ClassLoader getThrowawayClassLoader() {
		try {
			Object classFinder = this.getClassFinderMethod.invoke(this.classLoader);
			Object parent = this.getParentMethod.invoke(this.classLoader);
			// arguments for 'clone'-like method
			return (ClassLoader) this.wlGenericClassLoaderConstructor.newInstance(classFinder, parent);
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("WebLogic GenericClassLoader constructor failed", ex.getCause());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not construct WebLogic GenericClassLoader", ex);
		}
	}
}
