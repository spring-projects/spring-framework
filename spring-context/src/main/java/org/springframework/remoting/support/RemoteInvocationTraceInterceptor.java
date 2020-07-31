/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * AOP Alliance MethodInterceptor for tracing remote invocations.
 * Automatically applied by RemoteExporter and its subclasses.
 *
 * <p>Logs an incoming remote call as well as the finished processing of a remote call
 * at DEBUG level. If the processing of a remote call results in a checked exception,
 * the exception will get logged at INFO level; if it results in an unchecked
 * exception (or error), the exception will get logged at WARN level.
 *
 * <p>The logging of exceptions is particularly useful to save the stacktrace
 * information on the server-side rather than just propagating the exception
 * to the client (who might or might not log it properly).
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see RemoteExporter#setRegisterTraceInterceptor
 * @see RemoteExporter#getProxyForService
 */
public class RemoteInvocationTraceInterceptor implements MethodInterceptor {

	protected static final Log logger = LogFactory.getLog(RemoteInvocationTraceInterceptor.class);

	private final String exporterNameClause;


	/**
	 * Create a new RemoteInvocationTraceInterceptor.
	 */
	public RemoteInvocationTraceInterceptor() {
		this.exporterNameClause = "";
	}

	/**
	 * Create a new RemoteInvocationTraceInterceptor.
	 * @param exporterName the name of the remote exporter
	 * (to be used as context information in log messages)
	 */
	public RemoteInvocationTraceInterceptor(String exporterName) {
		this.exporterNameClause = exporterName + " ";
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		if (logger.isDebugEnabled()) {
			logger.debug("Incoming " + this.exporterNameClause + "remote call: " +
					ClassUtils.getQualifiedMethodName(method));
		}
		try {
			Object retVal = invocation.proceed();
			if (logger.isDebugEnabled()) {
				logger.debug("Finished processing of " + this.exporterNameClause + "remote call: " +
						ClassUtils.getQualifiedMethodName(method));
			}
			return retVal;
		}
		catch (Throwable ex) {
			if (ex instanceof RuntimeException || ex instanceof Error) {
				if (logger.isWarnEnabled()) {
					logger.warn("Processing of " + this.exporterNameClause + "remote call resulted in fatal exception: " +
							ClassUtils.getQualifiedMethodName(method), ex);
				}
			}
			else {
				if (logger.isInfoEnabled()) {
					logger.info("Processing of " + this.exporterNameClause + "remote call resulted in exception: " +
							ClassUtils.getQualifiedMethodName(method), ex);
				}
			}
			throw ex;
		}
	}

}
