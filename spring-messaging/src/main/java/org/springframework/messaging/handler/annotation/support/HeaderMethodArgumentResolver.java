/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

/**
 * Resolves method parameters annotated with {@link Header @Header}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	private static final Log logger = LogFactory.getLog(HeaderMethodArgumentResolver.class);


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
	protected Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name)
			throws Exception {

		Object headerValue = message.getHeaders().get(name);
		Object nativeHeaderValue = getNativeHeaderValue(message, name);

		if (headerValue != null && nativeHeaderValue != null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Message headers contain two values for the same header '" + name + "', " +
						"one in the top level header map and a second in the nested map with native headers. " +
						"Using the value from top level map. " +
						"Use 'nativeHeader.myHeader' to resolve to the value from the nested native header map." );
			}
		}

		return (headerValue != null ? headerValue : nativeHeaderValue);
	}

	private Object getNativeHeaderValue(Message<?> message, String name) {
		Map<String, List<String>> nativeHeaders = getNativeHeaders(message);
		if (name.startsWith("nativeHeaders.")) {
			name = name.substring("nativeHeaders.".length());
		}
		if (nativeHeaders == null || !nativeHeaders.containsKey(name)) {
			return null;
		}
		List<?> nativeHeaderValues = nativeHeaders.get(name);
		return (nativeHeaderValues.size() == 1 ? nativeHeaderValues.get(0) : nativeHeaderValues);
	}

	@SuppressWarnings("unchecked")
	private Map<String, List<String>> getNativeHeaders(Message<?> message) {
		return (Map<String, List<String>>) message.getHeaders().get(
				NativeMessageHeaderAccessor.NATIVE_HEADERS);
	}

	@Override
	protected void handleMissingValue(String headerName, MethodParameter parameter, Message<?> message) {
		throw new MessageHandlingException(message, "Missing header '" + headerName +
				"' for method parameter type [" + parameter.getParameterType() + "]");
	}


	private static class HeaderNamedValueInfo extends NamedValueInfo {

		private HeaderNamedValueInfo(Header annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
