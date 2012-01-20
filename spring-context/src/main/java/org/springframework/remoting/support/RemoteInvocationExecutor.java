/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.remoting.support;

import java.lang.reflect.InvocationTargetException;

/**
 * Strategy interface for executing a {@link RemoteInvocation} on a target object.
 *
 * <p>Used by {@link org.springframework.remoting.rmi.RmiServiceExporter} (for RMI invokers)
 * and by {@link org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter}.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see DefaultRemoteInvocationFactory
 * @see org.springframework.remoting.rmi.RmiServiceExporter#setRemoteInvocationExecutor
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter#setRemoteInvocationExecutor
 */
public interface RemoteInvocationExecutor {

	/**
	 * Perform this invocation on the given target object.
	 * Typically called when a RemoteInvocation is received on the server.
	 * @param invocation the RemoteInvocation
	 * @param targetObject the target object to apply the invocation to
	 * @return the invocation result
	 * @throws NoSuchMethodException if the method name could not be resolved
	 * @throws IllegalAccessException if the method could not be accessed
	 * @throws InvocationTargetException if the method invocation resulted in an exception
	 * @see java.lang.reflect.Method#invoke
	 */
	Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;

}
