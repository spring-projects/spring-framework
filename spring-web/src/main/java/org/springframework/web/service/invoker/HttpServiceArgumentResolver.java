/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Resolve an argument from an {@link HttpExchange @HttpExchange}-annotated method
 * to one or more HTTP request values.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public interface HttpServiceArgumentResolver {

	/**
	 * Resolve the argument value.
	 * @param argument the argument value
	 * @param parameter the method parameter for the argument
	 * @param requestValues builder to add HTTP request values to
	 * @return {@code true} if the argument was resolved, {@code false} otherwise
	 */
	boolean resolve(@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues);

}
