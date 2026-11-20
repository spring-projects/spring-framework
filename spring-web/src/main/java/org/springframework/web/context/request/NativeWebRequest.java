/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.request;

import org.jspecify.annotations.Nullable;

/**
 * Extension of the {@link WebRequest} interface, exposing the
 * native request and response objects in a generic fashion.
 *
 * <p>Mainly intended for framework-internal usage,
 * in particular for generic argument resolution code.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public interface NativeWebRequest extends WebRequest {

	/**
	 * Return the underlying native request object.
	 * @see jakarta.servlet.http.HttpServletRequest
	 */
	Object getNativeRequest();

	/**
	 * Return the underlying native response object, if any.
	 * @see jakarta.servlet.http.HttpServletResponse
	 */
	@Nullable Object getNativeResponse();

	/**
	 * Return the underlying native request object, if available.
	 * @param requiredType the desired type of request object
	 * @return the matching request object, or {@code null} if none
	 * of that type is available
	 * @see jakarta.servlet.http.HttpServletRequest
	 */
	<T> @Nullable T getNativeRequest(@Nullable Class<T> requiredType);

	/**
	 * Return the underlying native response object, if available.
	 * @param requiredType the desired type of response object
	 * @return the matching response object, or {@code null} if none
	 * of that type is available
	 * @see jakarta.servlet.http.HttpServletResponse
	 */
	<T> @Nullable T getNativeResponse(@Nullable Class<T> requiredType);

}
