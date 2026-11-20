/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.support;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

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

	private @Nullable ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
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
