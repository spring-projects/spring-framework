/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Optional;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolve method arguments annotated with {@code @CookieValue}.
 *
 * <p>An {@code @CookieValue} is a named value that is resolved from a cookie.
 * It has a required flag and a default value to fall back on when the cookie
 * does not exist.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class CookieValueMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {


	/**
	 * @param beanFactory a bean factory to use for resolving  ${...}
	 * placeholder and #{...} SpEL expressions in default values;
	 * or {@code null} if default values are not expected to contain expressions
	 */
	public CookieValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CookieValue.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		CookieValue annotation = parameter.getParameterAnnotation(CookieValue.class);
		return new CookieValueNamedValueInfo(annotation);
	}

	@Override
	protected Optional<Object> resolveNamedValue(String name, MethodParameter parameter,
			ServerWebExchange exchange) {

		HttpCookie cookie = exchange.getRequest().getCookies().getFirst(name);
		Class<?> paramType = parameter.getNestedParameterType();
		if (HttpCookie.class.isAssignableFrom(paramType)) {
			return Optional.ofNullable(cookie);
		}
		else if (cookie != null) {
			return Optional.ofNullable(cookie.getValue());
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String type = parameter.getNestedParameterType().getSimpleName();
		String reason = "Missing cookie '" + name + "' for method parameter of type " + type;
		throw new ServerWebInputException(reason, parameter);
	}


	private static class CookieValueNamedValueInfo extends NamedValueInfo {

		private CookieValueNamedValueInfo(CookieValue annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
