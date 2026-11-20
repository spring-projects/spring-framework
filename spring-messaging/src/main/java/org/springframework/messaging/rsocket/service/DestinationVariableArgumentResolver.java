/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.handler.annotation.DestinationVariable;

/**
 * {@link RSocketServiceArgumentResolver} for a
 * {@link DestinationVariable @DestinationVariable} annotated argument.
 *
 * <p>The argument is treated as a single route variable, or in case of a
 * Collection or an array, as multiple route variables.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class DestinationVariableArgumentResolver implements RSocketServiceArgumentResolver {

	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, RSocketRequestValues.Builder requestValues) {

		DestinationVariable annot = parameter.getParameterAnnotation(DestinationVariable.class);
		if (annot == null) {
			return false;
		}

		if (argument != null) {
			if (argument instanceof Collection<?> collection) {
				collection.forEach(requestValues::addRouteVariable);
				return true;
			}
			else if (argument instanceof Object[] arguments) {
				for (Object variable : arguments) {
					requestValues.addRouteVariable(variable);
				}
				return true;
			}
			else {
				requestValues.addRouteVariable(argument);
			}
		}

		return true;
	}

}
