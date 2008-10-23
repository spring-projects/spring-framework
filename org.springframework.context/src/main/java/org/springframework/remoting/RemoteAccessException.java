/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.remoting;

import org.springframework.core.NestedRuntimeException;

/**
 * Generic remote access exception. A service proxy for any remoting
 * protocol should throw this exception or subclasses of it, in order
 * to transparently expose a plain Java business interface.
 *
 * <p>When using conforming proxies, switching the actual remoting protocol
 * e.g. from Hessian to Burlap does not affect client code. Clients work
 * with a plain natural Java business interface that the service exposes.
 * A client object simply receives an implementation for the interface that
 * it needs via a bean reference, like it does for a local bean as well.
 *
 * <p>A client may catch RemoteAccessException if it wants to, but as
 * remote access errors are typically unrecoverable, it will probably let
 * such exceptions propagate to a higher level that handles them generically.
 * In this case, the client code doesn't show any signs of being involved in
 * remote access, as there aren't any remoting-specific dependencies.
 *
 * <p>Even when switching from a remote service proxy to a local implementation
 * of the same interface, this amounts to just a matter of configuration. Obviously,
 * the client code should be somewhat aware that it <i>might be working</i>
 * against a remote service, for example in terms of repeated method calls that
 * cause unnecessary roundtrips etc. However, it doesn't have to be aware whether
 * it is <i>actually working</i> against a remote service or a local implementation,
 * or with which remoting protocol it is working under the hood.
 *
 * @author Juergen Hoeller
 * @since 14.05.2003
 */
public class RemoteAccessException extends NestedRuntimeException {

	/** Use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = -4906825139312227864L;


	/**
	 * Constructor for RemoteAccessException.
	 * @param msg the detail message
	 */
	public RemoteAccessException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for RemoteAccessException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * remoting API such as RMI)
	 */
	public RemoteAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
