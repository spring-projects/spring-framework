/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.context.ApplicationEvent;
import org.springframework.util.ClassUtils;

/**
 * Event indicating a method invocation that failed.
 *
 * @author Juergen Hoeller
 * @since 7.0.3
 * @see EventPublicationInterceptor
 */
@SuppressWarnings("serial")
public class MethodFailureEvent extends ApplicationEvent {

	private final Throwable failure;


	/**
	 * Create a new event for the given method invocation.
	 * @param invocation the method invocation
	 * @param failure the exception encountered
	 */
	public MethodFailureEvent(MethodInvocation invocation, Throwable failure) {
		super(invocation);
		this.failure = failure;
	}


	/**
	 * Return the method invocation that triggered this event.
	 */
	@Override
	public MethodInvocation getSource() {
		return (MethodInvocation) super.getSource();
	}

	/**
	 * Return the method that triggered this event.
	 */
	public Method getMethod() {
		return getSource().getMethod();
	}

	/**
	 * Return the exception encountered.
	 */
	public Throwable getFailure() {
		return this.failure;
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + ClassUtils.getQualifiedMethodName(getMethod()) +
				" [" + getFailure() + "]";
	}

}
