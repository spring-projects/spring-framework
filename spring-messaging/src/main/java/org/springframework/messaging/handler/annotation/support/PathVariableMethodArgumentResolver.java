/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.PathVariable;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.messaging.simp.handler.AnnotationMethodMessageHandler;

import java.util.Map;

/**
 * Resolves method parameters annotated with {@link PathVariable @PathVariable}.
 *
 * <p>A @{@link PathVariable} is a named value that gets resolved from a path
 * template variable that matches the Message destination header.
 * It is always required and does not have a default value to fall back on.
 *
 * @author Brian Clozel
 * @see org.springframework.messaging.handler.annotation.PathVariable
 * @see org.springframework.messaging.MessageHeaders
 * @since 4.0
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	public PathVariableMethodArgumentResolver(ConversionService cs, ConfigurableBeanFactory beanFactory) {
		super(cs, beanFactory);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(PathVariable.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		PathVariable annotation = parameter.getParameterAnnotation(PathVariable.class);
		return new PathVariableNamedValueInfo(annotation);
	}

	@Override
	protected Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name) throws Exception {
		Map<String, String> pathTemplateVars =
				(Map<String, String>) message.getHeaders().get(AnnotationMethodMessageHandler.PATH_TEMPLATE_VARIABLES_HEADER);
		return (pathTemplateVars != null) ? pathTemplateVars.get(name) : null;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter, Message<?> message) {
		throw new MessageHandlingException(message, "Missing path template variable '" + name +
				"' for method parameter type [" + parameter.getParameterType() + "]");
	}

	private static class PathVariableNamedValueInfo extends NamedValueInfo {

		private PathVariableNamedValueInfo(PathVariable annotation) {
			super(annotation.value(), true, ValueConstants.DEFAULT_NONE);
		}
	}
}