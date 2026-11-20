/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation.support;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * Resolver for {@link Header @Header} arguments. Headers are resolved from
 * either the top-level header map or the nested
 * {@link NativeMessageHeaderAccessor native} header map.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see HeadersMethodArgumentResolver
 * @see NativeMessageHeaderAccessor
 */
public class HeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	private static final Log logger = LogFactory.getLog(HeaderMethodArgumentResolver.class);


	public HeaderMethodArgumentResolver(
			ConversionService conversionService, @Nullable ConfigurableBeanFactory beanFactory) {

		super(conversionService, beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Header.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		Header annot = parameter.getParameterAnnotation(Header.class);
		Assert.state(annot != null, "No Header annotation");
		return new HeaderNamedValueInfo(annot);
	}

	@Override
	protected @Nullable Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name)
			throws Exception {

		Object headerValue = message.getHeaders().get(name);
		Object nativeHeaderValue = getNativeHeaderValue(message, name);

		if (headerValue != null && nativeHeaderValue != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("A value was found for '" + name + "', in both the top level header map " +
						"and also in the nested map for native headers. Using the value from top level map. " +
						"Use 'nativeHeader.myHeader' to resolve the native header.");
			}
		}

		return (headerValue != null ? headerValue : nativeHeaderValue);
	}

	private @Nullable Object getNativeHeaderValue(Message<?> message, String name) {
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
	private @Nullable Map<String, List<String>> getNativeHeaders(Message<?> message) {
		return (Map<String, List<String>>) message.getHeaders().get(NativeMessageHeaderAccessor.NATIVE_HEADERS);
	}

	@Override
	protected void handleMissingValue(String headerName, MethodParameter parameter, Message<?> message) {
		throw new MessageHandlingException(message, "Missing header '" + headerName +
				"' for method parameter type [" + parameter.getParameterType() + "]");
	}


	private static final class HeaderNamedValueInfo extends NamedValueInfo {

		private HeaderNamedValueInfo(Header annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
