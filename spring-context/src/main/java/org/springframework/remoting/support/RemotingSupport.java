/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.remoting.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.util.ClassUtils;

/**
 * Generic support base class for remote accessor and exporters,
 * providing common bean ClassLoader handling.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public abstract class RemotingSupport implements BeanClassLoaderAware {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Return the ClassLoader that this accessor operates in,
	 * to be used for deserializing and for generating proxies.
	 */
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}


	/**
	 * Override the thread context ClassLoader with the environment's bean ClassLoader
	 * if necessary, i.e. if the bean ClassLoader is not equivalent to the thread
	 * context ClassLoader already.
	 * @return the original thread context ClassLoader, or {@code null} if not overridden
	 */
	protected ClassLoader overrideThreadContextClassLoader() {
		return ClassUtils.overrideThreadContextClassLoader(getBeanClassLoader());
	}

	/**
	 * Reset the original thread context ClassLoader if necessary.
	 * @param original the original thread context ClassLoader,
	 * or {@code null} if not overridden (and hence nothing to reset)
	 */
	protected void resetThreadContextClassLoader(ClassLoader original) {
		if (original != null) {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

}
