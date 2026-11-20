/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A factory for creating a {@link WebDataBinder} instance for a named target object.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface WebDataBinderFactory {

	/**
	 * Create a {@link WebDataBinder} for the given object.
	 * @param webRequest the current request
	 * @param target the object to create a data binder for,
	 * or {@code null} if creating a binder for a simple type
	 * @param objectName the name of the target object
	 * @return the created {@link WebDataBinder} instance, never null
	 * @throws Exception raised if the creation and initialization of the data binder fails
	 */
	WebDataBinder createBinder(NativeWebRequest webRequest, @Nullable Object target, String objectName)
			throws Exception;

	/**
	 * Variant of {@link #createBinder(NativeWebRequest, Object, String)} with a
	 * {@link ResolvableType} for which the {@code DataBinder} is created.
	 * This may be used to construct the target, or otherwise provide more
	 * insight on how to initialize the binder.
	 * @since 6.1
	 */
	default WebDataBinder createBinder(
			NativeWebRequest webRequest, @Nullable Object target, String objectName,
			ResolvableType targetType) throws Exception {

		return createBinder(webRequest, target, objectName);
	}


}
