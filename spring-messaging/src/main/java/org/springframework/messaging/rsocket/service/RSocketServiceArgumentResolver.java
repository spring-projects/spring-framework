/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;

/**
 * Resolve an argument from an {@link RSocketExchange @RSocketExchange}-annotated
 * method to one or more RSocket request values.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public interface RSocketServiceArgumentResolver {

	/**
	 * Resolve the argument value.
	 * @param argument the argument value
	 * @param parameter the method parameter for the argument
	 * @param requestValues builder to add RSocket request values to
	 * @return {@code true} if the argument was resolved, {@code false} otherwise
	 */
	boolean resolve(@Nullable Object argument, MethodParameter parameter, RSocketRequestValues.Builder requestValues);

}
