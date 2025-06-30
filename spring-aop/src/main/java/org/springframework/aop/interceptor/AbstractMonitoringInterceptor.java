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

package org.springframework.aop.interceptor;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

/**
 * Base class for monitoring interceptors, such as performance monitors.
 * Provides configurable "prefix and "suffix" properties that help to
 * classify/group performance monitoring results.
 *
 * <p>In their {@link #invokeUnderTrace} implementation, subclasses should call the
 * {@link #createInvocationTraceName} method to create a name for the given trace,
 * including information about the method invocation along with a prefix/suffix.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2.7
 * @see #setPrefix
 * @see #setSuffix
 * @see #createInvocationTraceName
 */
@SuppressWarnings("serial")
public abstract class AbstractMonitoringInterceptor extends AbstractTraceInterceptor {

	private String prefix = "";

	private String suffix = "";

	private boolean logTargetClassInvocation = false;


	/**
	 * Set the text that will get appended to the trace data.
	 * <p>Default is none.
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * Return the text that will get appended to the trace data.
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * Set the text that will get prepended to the trace data.
	 * <p>Default is none.
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * Return the text that will get prepended to the trace data.
	 */
	protected String getSuffix() {
		return this.suffix;
	}

	/**
	 * Set whether to log the invocation on the target class, if applicable
	 * (i.e. if the method is actually delegated to the target class).
	 * <p>Default is "false", logging the invocation based on the proxy
	 * interface/class name.
	 */
	public void setLogTargetClassInvocation(boolean logTargetClassInvocation) {
		this.logTargetClassInvocation = logTargetClassInvocation;
	}


	/**
	 * Create a {@code String} name for the given {@code MethodInvocation}
	 * that can be used for trace/logging purposes. This name is made up of the
	 * configured prefix, followed by the fully-qualified name of the method being
	 * invoked, followed by the configured suffix.
	 * @see #setPrefix
	 * @see #setSuffix
	 */
	protected String createInvocationTraceName(MethodInvocation invocation) {
		Method method = invocation.getMethod();
		Class<?> clazz = method.getDeclaringClass();
		if (this.logTargetClassInvocation && clazz.isInstance(invocation.getThis())) {
			clazz = invocation.getThis().getClass();
		}
		String className = clazz.getName();
		return getPrefix() + className + '.' + method.getName() + getSuffix();
	}

}
