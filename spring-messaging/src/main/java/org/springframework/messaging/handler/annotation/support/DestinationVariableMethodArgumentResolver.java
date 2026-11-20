/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation.support;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
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
	@SuppressWarnings("unchecked")
	protected @Nullable Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name) {
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
