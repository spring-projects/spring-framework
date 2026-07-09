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
