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

package org.springframework.scheduling.support;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.util.ClassUtils;

/**
 * Adapter that implements the {@link Runnable} interface as a configurable
 * method invocation based on Spring's MethodInvoker.
 *
 * <p>Inherits common configuration properties from
 * {@link org.springframework.util.MethodInvoker}.
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see java.util.concurrent.Executor#execute(Runnable)
 */
public class MethodInvokingRunnable extends ArgumentConvertingMethodInvoker
		implements Runnable, BeanClassLoaderAware, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	protected Class resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, this.beanClassLoader);
	}

	@Override
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
		prepare();
	}


	@Override
	public void run() {
		try {
			invoke();
		}
		catch (InvocationTargetException ex) {
			logger.error(getInvocationFailureMessage(), ex.getTargetException());
			// Do not throw exception, else the main loop of the scheduler might stop!
		}
		catch (Throwable ex) {
			logger.error(getInvocationFailureMessage(), ex);
			// Do not throw exception, else the main loop of the scheduler might stop!
		}
	}

	/**
	 * Build a message for an invocation failure exception.
	 * @return the error message, including the target method name etc
	 */
	protected String getInvocationFailureMessage() {
		return "Invocation of method '" + getTargetMethod() +
				"' on target class [" + getTargetClass() + "] failed";
	}

}
