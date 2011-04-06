/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.method.annotation.support;

import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} that supports arguments annotated with
 * {@link RequestHeader @RequestHeader}.
 *
 * @author Arjen Poutsma
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	public RequestHeaderMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}

	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestHeader.class)
				&& !Map.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestHeader annotation = parameter.getParameterAnnotation(RequestHeader.class);
		return new RequestHeaderNamedValueInfo(annotation);
	}

	@Override
	protected Object resolveNamedValueArgument(NativeWebRequest webRequest,
											   MethodParameter parameter,
											   String headerName) throws Exception {
		String[] headerValues = webRequest.getHeaderValues(headerName);
		if (headerValues != null) {
			return (headerValues.length == 1 ? headerValues[0] : headerValues);
		}
		else {
			return null;
		}
	}

	@Override
	protected void handleMissingValue(String headerName, MethodParameter parameter) {
		throw new IllegalStateException(
				"Missing header '" + headerName + "' of type [" + parameter.getParameterType().getName() + "]");
	}

	private static class RequestHeaderNamedValueInfo extends NamedValueInfo {

		private RequestHeaderNamedValueInfo(RequestHeader annotation) {
			super(annotation.value(), annotation.required(), annotation.defaultValue());
		}
	}

}
