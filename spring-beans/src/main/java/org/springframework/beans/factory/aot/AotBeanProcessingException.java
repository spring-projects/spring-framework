/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Thrown when AOT fails to process a bean.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
@SuppressWarnings("serial")
public class AotBeanProcessingException extends AotProcessingException {

	private final RootBeanDefinition beanDefinition;


	/**
	 * Create an instance with the {@link RegisteredBean} that fails to be
	 * processed, a detail message, and an optional root cause.
	 * @param registeredBean the registered bean that fails to be processed
	 * @param msg the detail message
	 * @param cause the root cause, if any
	 */
	public AotBeanProcessingException(RegisteredBean registeredBean, String msg, @Nullable Throwable cause) {
		super(createErrorMessage(registeredBean, msg), cause);
		this.beanDefinition = registeredBean.getMergedBeanDefinition();
	}

	/**
	 * Shortcut to create an instance with the {@link RegisteredBean} that fails
	 * to be processed with only a detail message.
	 * @param registeredBean the registered bean that fails to be processed
	 * @param msg the detail message
	 */
	public AotBeanProcessingException(RegisteredBean registeredBean, String msg) {
		this(registeredBean, msg, null);
	}

	private static String createErrorMessage(RegisteredBean registeredBean, String msg) {
		StringBuilder sb = new StringBuilder("Error processing bean with name '");
		sb.append(registeredBean.getBeanName()).append("'");
		String resourceDescription = registeredBean.getMergedBeanDefinition().getResourceDescription();
		if (resourceDescription != null) {
			sb.append(" defined in ").append(resourceDescription);
		}
		sb.append(": ").append(msg);
		return sb.toString();
	}


	/**
	 * Return the bean definition of the bean that failed to be processed.
	 */
	public RootBeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}

}
