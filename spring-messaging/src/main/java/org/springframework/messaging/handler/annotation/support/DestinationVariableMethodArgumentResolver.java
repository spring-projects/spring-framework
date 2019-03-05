/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.util.Assert;

/**
 * Resolve for {@link DestinationVariable @DestinationVariable} method parameters.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public class DestinationVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/** The name of the header used to for template variables. */
	public static final String DESTINATION_TEMPLATE_VARIABLES_HEADER =
			DestinationVariableMethodArgumentResolver.class.getSimpleName() + ".templateVariables";


	public DestinationVariableMethodArgumentResolver(ConversionService conversionService) {
		super(conversionService, null);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(DestinationVariable.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		DestinationVariable annot = parameter.getParameterAnnotation(DestinationVariable.class);
		Assert.state(annot != null, "No DestinationVariable annotation");
		return new DestinationVariableNamedValueInfo(annot);
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	protected Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name) {
		MessageHeaders headers = message.getHeaders();
		Map<String, String> vars = (Map<String, String>) headers.get(DESTINATION_TEMPLATE_VARIABLES_HEADER);
		return vars != null ? vars.get(name) : null;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter, Message<?> message) {
		throw new MessageHandlingException(message, "Missing path template variable '" + name + "' " +
				"for method parameter type [" + parameter.getParameterType() + "]");
	}


	private static final class DestinationVariableNamedValueInfo extends NamedValueInfo {

		private DestinationVariableNamedValueInfo(DestinationVariable annotation) {
			super(annotation.value(), true, ValueConstants.DEFAULT_NONE);
		}
	}

}
