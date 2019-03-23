/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

/**
 * Encapsulates a remote invocation result, holding a result value or an exception.
 * Used for HTTP-based serialization invokers.
 *
 * <p>This is an SPI class, typically not used directly by applications.
 * Can be subclassed for additional invocation parameters.
 *
 * <p>Both {@link RemoteInvocation} and {@link RemoteInvocationResult} are designed
 * for use with standard Java serialization as well as JavaBean-style serialization.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see RemoteInvocation
 */
public class RemoteInvocationResult implements Serializable {

	/** Use serialVersionUID from Spring 1.1 for interoperability */
	private static final long serialVersionUID = 2138555143707773549L;


	private Object value;

	private Throwable exception;


	/**
	 * Create a new RemoteInvocationResult for the given result value.
	 * @param value the result value returned by a successful invocation
	 * of the target method
	 */
	public RemoteInvocationResult(Object value) {
		this.value = value;
	}

	/**
	 * Create a new RemoteInvocationResult for the given exception.
	 * @param exception the exception thrown by an unsuccessful invocation
	 * of the target method
	 */
	public RemoteInvocationResult(Throwable exception) {
		this.exception = exception;
	}

	/**
	 * Create a new RemoteInvocationResult for JavaBean-style deserialization
	 * (e.g. with Jackson).
	 * @see #setValue
	 * @see #setException
	 */
	public RemoteInvocationResult() {
	}


	/**
	 * Set the result value returned by a successful invocation of the
	 * target method, if any.
	 * <p>This setter is intended for JavaBean-style deserialization.
	 * Use {@link #RemoteInvocationResult(Object)} otherwise.
	 * @see #RemoteInvocationResult()
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Return the result value returned by a successful invocation
	 * of the target method, if any.
	 * @see #hasException
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Set the exception thrown by an unsuccessful invocation of the
	 * target method, if any.
	 * <p>This setter is intended for JavaBean-style deserialization.
	 * Use {@link #RemoteInvocationResult(Throwable)} otherwise.
	 * @see #RemoteInvocationResult()
	 */
	public void setException(Throwable exception) {
		this.exception = exception;
	}

	/**
	 * Return the exception thrown by an unsuccessful invocation
	 * of the target method, if any.
	 * @see #hasException
	 */
	public Throwable getException() {
		return this.exception;
	}

	/**
	 * Return whether this invocation result holds an exception.
	 * If this returns {@code false}, the result value applies
	 * (even if it is {@code null}).
	 * @see #getValue
	 * @see #getException
	 */
	public boolean hasException() {
		return (this.exception != null);
	}

	/**
	 * Return whether this invocation result holds an InvocationTargetException,
	 * thrown by an invocation of the target method itself.
	 * @see #hasException()
	 */
	public boolean hasInvocationTargetException() {
		return (this.exception instanceof InvocationTargetException);
	}


	/**
	 * Recreate the invocation result, either returning the result value
	 * in case of a successful invocation of the target method, or
	 * rethrowing the exception thrown by the target method.
	 * @return the result value, if any
	 * @throws Throwable the exception, if any
	 */
	public Object recreate() throws Throwable {
		if (this.exception != null) {
			Throwable exToThrow = this.exception;
			if (this.exception instanceof InvocationTargetException) {
				exToThrow = ((InvocationTargetException) this.exception).getTargetException();
			}
			RemoteInvocationUtils.fillInClientStackTraceIfPossible(exToThrow);
			throw exToThrow;
		}
		else {
			return this.value;
		}
	}

}
