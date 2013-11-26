/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.messaging.handler.annotation.Header;

/**
 * Resolves method parameters annotated with {@link Header @Header}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {


	public HeaderMethodArgumentResolver(ConversionService cs, ConfigurableBeanFactory beanFactory) {
		super(cs, beanFactory);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Header.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		Header annotation = parameter.getParameterAnnotation(Header.class);
		return new HeaderNamedValueInfo(annotation);
	}

	@Override
	protected Object resolveArgumentInternal(MethodParameter parameter, Message<?> message,
			String name) throws Exception {

		return message.getHeaders().get(name);
	}

	@Override
	protected void handleMissingValue(String headerName, MethodParameter parameter, Message<?> message) {
		throw new MessageHandlingException(message, "Missing header '" + headerName +
				"' for method parameter type [" + parameter.getParameterType() + "]");
	}


	private static class HeaderNamedValueInfo extends NamedValueInfo {

		private HeaderNamedValueInfo(Header annotation) {
			super(annotation.value(), annotation.required(), annotation.defaultValue());
		}
	}

}
