/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.method.annotation;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;

/**
 * A base abstract class to resolve method arguments annotated with
 * {@code @CookieValue}. Subclasses extract the cookie value from the request.
 *
 * <p>An {@code @CookieValue} is a named value that is resolved from a cookie.
 * It has a required flag and a default value to fall back on when the cookie
 * does not exist.
 *
 * <p>A {@link WebDataBinder} may be invoked to apply type conversion to the
 * resolved cookie value.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractCookieValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * @param beanFactory a bean factory to use for resolving  ${...}
	 * placeholder and #{...} SpEL expressions in default values;
	 * or {@code null} if default values are not expected to contain expressions
	 */
	public AbstractCookieValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}

	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CookieValue.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		CookieValue annotation = parameter.getParameterAnnotation(CookieValue.class);
		return new CookieValueNamedValueInfo(annotation);
	}

	@Override
	protected void handleMissingValue(String cookieName, MethodParameter param) throws ServletRequestBindingException {
		String paramType = param.getParameterType().getName();
		throw new ServletRequestBindingException(
				"Missing cookie named '" + cookieName + "' for method parameter type [" + paramType + "]");
	}

	private static class CookieValueNamedValueInfo extends NamedValueInfo {

		private CookieValueNamedValueInfo(CookieValue annotation) {
			super(annotation.value(), annotation.required(), annotation.defaultValue());
		}
	}
}