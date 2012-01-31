/*
 * Copyright 2002-2005 the original author or authors.
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

/**
 * Configuration interface for executing HTTP invoker requests.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see HttpInvokerRequestExecutor
 * @see HttpInvokerClientInterceptor
 */
public interface HttpInvokerClientConfiguration {

	/**
	 * Return the HTTP URL of the target service.
	 */
	String getServiceUrl();

	/**
	 * Return the codebase URL to download classes from if not found locally.
	 * Can consist of multiple URLs, separated by spaces.
	 * @return the codebase URL, or <code>null</code> if none
	 * @see java.rmi.server.RMIClassLoader
	 */
	String getCodebaseUrl();

}
