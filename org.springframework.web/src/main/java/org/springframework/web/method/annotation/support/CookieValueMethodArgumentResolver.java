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

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.util.UrlPathHelper;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} that supports arguments annotated with
 * {@link CookieValue @CookieValue}.
 *
 * @author Arjen Poutsma
 */
public class CookieValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	public CookieValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}

	public UrlPathHelper getUrlPathHelper() {
		return urlPathHelper;
	}

	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
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
	protected Object resolveNamedValueArgument(NativeWebRequest webRequest,
											   MethodParameter parameter,
											   String cookieName) throws Exception {

		throw new UnsupportedOperationException("@CookieValue not supported");
	}

	@Override
	protected void handleMissingValue(String cookieName, MethodParameter parameter) {
		throw new IllegalStateException(
				"Missing cookie value '" + cookieName + "' of type [" + parameter.getParameterType().getName() + "]");
	}

	private static class CookieValueNamedValueInfo extends NamedValueInfo {

		private CookieValueNamedValueInfo(CookieValue annotation) {
			super(annotation.value(), annotation.required(), annotation.defaultValue());
		}
	}
}
