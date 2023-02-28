/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.util;

import jakarta.servlet.ServletException;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.lang.Nullable;

/**
 * Legacy subclass of {@link ServletException} that handles a root cause in terms
 * of message and stacktrace.
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @deprecated as of 6.0, in favor of standard {@link ServletException} nesting
 */
@Deprecated(since = "6.0")
public class NestedServletException extends ServletException {

	/** Use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = -5292377985529381145L;

	static {
		// Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
		// issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
		NestedExceptionUtils.class.getName();
	}


	/**
	 * Construct a {@code NestedServletException} with the specified detail message.
	 * @param msg the detail message
	 */
	public NestedServletException(String msg) {
		super(msg);
	}

	/**
	 * Construct a {@code NestedServletException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public NestedServletException(@Nullable String msg, @Nullable Throwable cause) {
		super(NestedExceptionUtils.buildMessage(msg, cause), cause);
	}

}
