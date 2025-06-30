/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * An {@link org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver}
 * that resolves cookie values from an {@link HttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletCookieValueMethodArgumentResolver extends AbstractCookieValueMethodArgumentResolver {

	private UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;


	public ServletCookieValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	/**
	 * Whether to apply URL decoding to cookie values via
	 * {@link UrlPathHelper#decodeRequestString(HttpServletRequest, String)}.
	 * A shortcut for doing the same by setting a {@link UrlPathHelper} with
	 * its {@code urlDecode} property set accordingly.
	 * <p>By default, set to "true" in which case cookie values are decoded.
	 * @since 6.1.2
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper = (urlDecode ? UrlPathHelper.defaultInstance : UrlPathHelper.rawPathInstance);
	}

	/**
	 * Set the {@code UrlPathHelper} to use to decode cookie values with via
	 * {@link UrlPathHelper#decodeRequestString(HttpServletRequest, String)}.
	 * For most cases you can use {@link #setUrlDecode(boolean)} instead.
	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
	 * for use at runtime in web modules in favor of parsed patterns with
	 * {@link PathPatternParser}.
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}


	@Override
	protected @Nullable Object resolveName(String cookieName, MethodParameter parameter,
			NativeWebRequest webRequest) throws Exception {

		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		Assert.state(servletRequest != null, "No HttpServletRequest");

		Cookie cookieValue = WebUtils.getCookie(servletRequest, cookieName);
		if (Cookie.class.isAssignableFrom(parameter.getNestedParameterType())) {
			return cookieValue;
		}
		else if (cookieValue != null) {
			return this.urlPathHelper.decodeRequestString(servletRequest, cookieValue.getValue());
		}
		else {
			return null;
		}
	}

}
