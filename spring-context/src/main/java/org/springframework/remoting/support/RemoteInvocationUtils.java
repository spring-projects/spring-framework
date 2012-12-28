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

package org.springframework.remoting.support;

import java.util.HashSet;
import java.util.Set;

import org.springframework.core.JdkVersion;

/**
 * General utilities for handling remote invocations.
 *
 * <p>Mainly intended for use within the remoting framework.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class RemoteInvocationUtils {

	/**
	 * Fill the current client-side stack trace into the given exception.
	 * <p>The given exception is typically thrown on the server and serialized
	 * as-is, with the client wanting it to contain the client-side portion
	 * of the stack trace as well. What we can do here is to update the
	 * <code>StackTraceElement</code> array with the current client-side stack
	 * trace, provided that we run on JDK 1.4+.
	 * @param ex the exception to update
	 * @see java.lang.Throwable#getStackTrace()
	 * @see java.lang.Throwable#setStackTrace(StackTraceElement[])
	 */
	public static void fillInClientStackTraceIfPossible(Throwable ex) {
		if (ex != null) {
			StackTraceElement[] clientStack = new Throwable().getStackTrace();
			Set<Throwable> visitedExceptions = new HashSet<Throwable>();
			Throwable exToUpdate = ex;
			while (exToUpdate != null && !visitedExceptions.contains(exToUpdate)) {
				StackTraceElement[] serverStack = exToUpdate.getStackTrace();
				StackTraceElement[] combinedStack = new StackTraceElement[serverStack.length + clientStack.length];
				System.arraycopy(serverStack, 0, combinedStack, 0, serverStack.length);
				System.arraycopy(clientStack, 0, combinedStack, serverStack.length, clientStack.length);
				exToUpdate.setStackTrace(combinedStack);
				visitedExceptions.add(exToUpdate);
				exToUpdate = exToUpdate.getCause();
			}
		}
	}

}
