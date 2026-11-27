/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.Assert;

/**
 * Raised when {@code ServletRequest.getParameter*} fails while parsing the
 * request parameters.
 * <p>Servlet 6.1 defines that failure as an {@link IllegalStateException}.
 * Containers attach the status they will send once the exception leaves the
 * application: Tomcat's {@code InvalidParameterException} exposes it as
 * {@code getErrorCode()} (400, or 413 when the body is too large) and Jetty's
 * {@code HttpException} exposes it as {@code getCode()}. When the exception
 * carries no such status, this is a 400. Because this is an
 * {@link ErrorResponse}, MVC reports that status instead of letting the
 * failure escape as a 500.
 *
 * @author Lordwill Kandiro
 * @since 7.1.1
 */
@SuppressWarnings("serial")
public class InvalidServletRequestParameterException extends ErrorResponseException {

	/**
	 * Create an exception for a parameter-parsing failure.
	 * @param cause the exception thrown by {@code getParameter*}
	 */
	public InvalidServletRequestParameterException(IllegalStateException cause) {
		super(statusOf(cause), cause);
		setDetail("Failed to parse the request parameters");
	}


	/**
	 * Read request parameters, translating a parsing failure.
	 * @param read the parameter read to attempt
	 * @param <T> the type of the read
	 * @return the result of {@code read}
	 */
	public static <T> T read(Supplier<T> read) {
		Assert.notNull(read, "Parameter read must not be null");
		try {
			return read.get();
		}
		catch (IllegalStateException ex) {
			throw new InvalidServletRequestParameterException(ex);
		}
	}

	private static HttpStatusCode statusOf(IllegalStateException failure) {
		Throwable current = failure;
		while (current != null) {
			Integer code = statusCode(current);
			if (code != null && code >= 400 && code < 600) {
				return HttpStatusCode.valueOf(code);
			}
			Throwable cause = current.getCause();
			current = (cause == current ? null : cause);
		}
		return HttpStatus.BAD_REQUEST;
	}

	private static @Nullable Integer statusCode(Throwable failure) {
		for (String name : new String[] {"getErrorCode", "getCode", "getStatus"}) {
			try {
				Method method = failure.getClass().getMethod(name);
				if (method.getParameterCount() == 0 &&
						(method.getReturnType() == int.class || method.getReturnType() == Integer.class)) {
					Object value = method.invoke(failure);
					if (value instanceof Integer code) {
						return code;
					}
				}
			}
			catch (ReflectiveOperationException ex) {
				// The exception has no status under this name.
			}
		}
		return null;
	}

}
