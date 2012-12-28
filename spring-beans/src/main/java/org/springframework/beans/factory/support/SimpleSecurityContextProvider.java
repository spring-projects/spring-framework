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

package org.springframework.beans.factory.support;

import java.security.AccessControlContext;
import java.security.AccessController;

/**
 * Simple {@link SecurityContextProvider} implementation.
 *
 * @author Costin Leau
 * @since 3.0
 */
public class SimpleSecurityContextProvider implements SecurityContextProvider {

	private final AccessControlContext acc;


	/**
	 * Construct a new {@code SimpleSecurityContextProvider} instance.
	 * <p>The security context will be retrieved on each call from the current
	 * thread.
	 */
	public SimpleSecurityContextProvider() {
		this(null);
	}

	/**
	 * Construct a new {@code SimpleSecurityContextProvider} instance.
	 * <p>If the given control context is null, the security context will be
	 * retrieved on each call from the current thread.
	 * @param acc access control context (can be {@code null})
	 * @see AccessController#getContext()
	 */
	public SimpleSecurityContextProvider(AccessControlContext acc) {
		this.acc = acc;
	}


	public AccessControlContext getAccessControlContext() {
		return (this.acc != null ? acc : AccessController.getContext());
	}

}
