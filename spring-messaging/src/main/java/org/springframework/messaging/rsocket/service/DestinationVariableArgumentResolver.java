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

package org.springframework.messaging.rsocket.service;

import java.util.Collection;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
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
			else if (argument.getClass().isArray()) {
				for (Object variable : (Object[]) argument) {
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
