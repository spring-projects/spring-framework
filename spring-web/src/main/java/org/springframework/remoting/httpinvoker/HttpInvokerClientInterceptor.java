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

package org.springframework.remoting.httpinvoker;

import java.io.IOException;
import java.io.InvalidClassException;
import java.net.ConnectException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} for accessing an
 * HTTP invoker service. The service URL must be an HTTP URL exposing
 * an HTTP invoker service.
 *
 * <p>Serializes remote invocation objects and deserializes remote invocation
 * result objects. Uses Java serialization just like RMI, but provides the
 * same ease of setup as Caucho's HTTP-based Hessian and Burlap protocols.
 *
 * <P>HTTP invoker is a very extensible and customizable protocol.
 * It supports the RemoteInvocationFactory mechanism, like RMI invoker,
 * allowing to include additional invocation attributes (for example,
 * a security context). Furthermore, it allows to customize request
 * execution via the {@link HttpInvokerRequestExecutor} strategy.
 *
 * <p>Can use the JDK's {@link java.rmi.server.RMIClassLoader} to load
 * classes from a given {@link #setCodebaseUrl codebase}, performing
 * on-demand dynamic code download from a remote location. The codebase
 * can consist of multiple URLs, separated by spaces. Note that
 * RMIClassLoader requires a SecurityManager to be set, analogous to
 * when using dynamic class download with standard RMI!
 * (See the RMI documentation for details.)
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setServiceUrl
 * @see #setCodebaseUrl
 * @see #setRemoteInvocationFactory
 * @see #setHttpInvokerRequestExecutor
 * @see HttpInvokerServiceExporter
 * @see HttpInvokerProxyFactoryBean
 * @see java.rmi.server.RMIClassLoader
 */
public class HttpInvokerClientInterceptor extends RemoteInvocationBasedAccessor
		implements MethodInterceptor, HttpInvokerClientConfiguration {

	private String codebaseUrl;

	private HttpInvokerRequestExecutor httpInvokerRequestExecutor;


	/**
	 * Set the codebase URL to download classes from if not found locally.
	 * Can consists of multiple URLs, separated by spaces.
	 * <p>Follows RMI's codebase conventions for dynamic class download.
	 * In contrast to RMI, where the server determines the URL for class download
	 * (via the "java.rmi.server.codebase" system property), it's the client
	 * that determines the codebase URL here. The server will usually be the
	 * same as for the service URL, just pointing to a different path there.
	 * @see #setServiceUrl
	 * @see org.springframework.remoting.rmi.CodebaseAwareObjectInputStream
	 * @see java.rmi.server.RMIClassLoader
	 */
	public void setCodebaseUrl(String codebaseUrl) {
		this.codebaseUrl = codebaseUrl;
	}

	/**
	 * Return the codebase URL to download classes from if not found locally.
	 */
	@Override
	public String getCodebaseUrl() {
		return this.codebaseUrl;
	}

	/**
	 * Set the HttpInvokerRequestExecutor implementation to use for executing
	 * remote invocations.
	 * <p>Default is {@link SimpleHttpInvokerRequestExecutor}. Alternatively,
	 * consider using {@link HttpComponentsHttpInvokerRequestExecutor} for more
	 * sophisticated needs.
	 * @see SimpleHttpInvokerRequestExecutor
	 * @see HttpComponentsHttpInvokerRequestExecutor
	 */
	public void setHttpInvokerRequestExecutor(HttpInvokerRequestExecutor httpInvokerRequestExecutor) {
		this.httpInvokerRequestExecutor = httpInvokerRequestExecutor;
	}

	/**
	 * Return the HttpInvokerRequestExecutor used by this remote accessor.
	 * <p>Creates a default SimpleHttpInvokerRequestExecutor if no executor
	 * has been initialized already.
	 */
	public HttpInvokerRequestExecutor getHttpInvokerRequestExecutor() {
		if (this.httpInvokerRequestExecutor == null) {
			SimpleHttpInvokerRequestExecutor executor = new SimpleHttpInvokerRequestExecutor();
			executor.setBeanClassLoader(getBeanClassLoader());
			this.httpInvokerRequestExecutor = executor;
		}
		return this.httpInvokerRequestExecutor;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// Eagerly initialize the default HttpInvokerRequestExecutor, if needed.
		getHttpInvokerRequestExecutor();
	}


	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "HTTP invoker proxy for service URL [" + getServiceUrl() + "]";
		}

		RemoteInvocation invocation = createRemoteInvocation(methodInvocation);
		RemoteInvocationResult result = null;
		try {
			result = executeRequest(invocation, methodInvocation);
		}
		catch (Throwable ex) {
			throw convertHttpInvokerAccessException(ex);
		}
		try {
			return recreateRemoteInvocationResult(result);
		}
		catch (Throwable ex) {
			if (result.hasInvocationTargetException()) {
				throw ex;
			}
			else {
				throw new RemoteInvocationFailureException("Invocation of method [" + methodInvocation.getMethod() +
						"] failed in HTTP invoker remote service at [" + getServiceUrl() + "]", ex);
			}
		}
	}

	/**
	 * Execute the given remote invocation via the HttpInvokerRequestExecutor.
	 * <p>This implementation delegates to {@link #executeRequest(RemoteInvocation)}.
	 * Can be overridden to react to the specific original MethodInvocation.
	 * @param invocation the RemoteInvocation to execute
	 * @param originalInvocation the original MethodInvocation (can e.g. be cast
	 * to the ProxyMethodInvocation interface for accessing user attributes)
	 * @return the RemoteInvocationResult object
	 * @throws Exception in case of errors
	 */
	protected RemoteInvocationResult executeRequest(
			RemoteInvocation invocation, MethodInvocation originalInvocation) throws Exception {

		return executeRequest(invocation);
	}

	/**
	 * Execute the given remote invocation via the HttpInvokerRequestExecutor.
	 * <p>Can be overridden in subclasses to pass a different configuration object
	 * to the executor. Alternatively, add further configuration properties in a
	 * subclass of this accessor: By default, the accessor passed itself as
	 * configuration object to the executor.
	 * @param invocation the RemoteInvocation to execute
	 * @return the RemoteInvocationResult object
	 * @throws IOException if thrown by I/O operations
	 * @throws ClassNotFoundException if thrown during deserialization
	 * @throws Exception in case of general errors
	 * @see #getHttpInvokerRequestExecutor
	 * @see HttpInvokerClientConfiguration
	 */
	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation) throws Exception {
		return getHttpInvokerRequestExecutor().executeRequest(this, invocation);
	}

	/**
	 * Convert the given HTTP invoker access exception to an appropriate
	 * Spring RemoteAccessException.
	 * @param ex the exception to convert
	 * @return the RemoteAccessException to throw
	 */
	protected RemoteAccessException convertHttpInvokerAccessException(Throwable ex) {
		if (ex instanceof ConnectException) {
			return new RemoteConnectFailureException(
					"Could not connect to HTTP invoker remote service at [" + getServiceUrl() + "]", ex);
		}

		if (ex instanceof ClassNotFoundException || ex instanceof NoClassDefFoundError ||
				ex instanceof InvalidClassException) {
			return new RemoteAccessException(
					"Could not deserialize result from HTTP invoker remote service [" + getServiceUrl() + "]", ex);
		}

		return new RemoteAccessException(
					"Could not access HTTP invoker remote service at [" + getServiceUrl() + "]", ex);
	}

}
