/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core;

import org.springframework.lang.Nullable;

/**
 * Helper class for implementing exception classes which are capable of
 * holding nested exceptions. Necessary because we can't share a base
 * class among different exception types.
 *
 * <p>Mainly for use within the framework.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see NestedRuntimeException
 * @see NestedCheckedException
 */
public abstract class NestedExceptionUtils {

	/**
	 * Build a message for the given base message and root cause.
	 * @param message the base message
	 * @param cause the root cause
	 * @return the full exception message
	 * @deprecated as of 6.0, in favor of custom exception messages
	 * with selective inclusion of cause messages
	 */
	@Deprecated(since = "6.0")
	@Nullable
	public static String buildMessage(@Nullable String message, @Nullable Throwable cause) {
		if (cause == null) {
			return message;
		}
		StringBuilder sb = new StringBuilder(64);
		if (message != null) {
			sb.append(message).append("; ");
		}
		sb.append("nested exception is ").append(cause);
		return sb.toString();
	}

	/**
	 * Retrieve the innermost cause of the given exception, if any.
	 * @param original the original exception to introspect
	 * @return the innermost exception, or {@code null} if none
	 * @since 4.3.9
	 */
	@Nullable
	public static Throwable getRootCause(@Nullable Throwable original) {
		if (original == null) {
			return null;
		}
		Throwable rootCause = null;
		Throwable cause = original.getCause();
		while (cause != null && cause != rootCause) {
			rootCause = cause;
			cause = cause.getCause();
		}
		return rootCause;
	}

	/**
	 * Retrieve the most specific cause of the given exception, that is,
	 * either the innermost cause (root cause) or the exception itself.
	 * <p>Differs from {@link #getRootCause} in that it falls back
	 * to the original exception if there is no root cause.
	 * @param original the original exception to introspect
	 * @return the most specific cause (never {@code null})
	 * @since 4.3.9
	 */
	public static Throwable getMostSpecificCause(Throwable original) {
		Throwable rootCause = getRootCause(original);
		return (rootCause != null ? rootCause : original);
	}

}
