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

import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} that supports arguments annotated 
 * with {@link Value @Value}.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ExpressionValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	public ExpressionValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}

	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Value.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		Value annotation = parameter.getParameterAnnotation(Value.class);
		return new ExpressionValueNamedValueInfo(annotation);
	}

	@Override
	protected Object resolveNamedValueArgument(NativeWebRequest webRequest, MethodParameter parameter, String name)
			throws Exception {
		// Only interested in default value resolution
		return null;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		// Should not happen
		throw new UnsupportedOperationException();
	}

	private static class ExpressionValueNamedValueInfo extends NamedValueInfo {

		private ExpressionValueNamedValueInfo(Value annotation) {
			super("@Value", false, annotation.value());
		}
	}

}
