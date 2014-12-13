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
package org.springframework.web.servlet.view.tiles3;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;

import org.apache.tiles.locale.impl.DefaultLocaleResolver;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.servlet.NotAServletEnvironmentException;
import org.apache.tiles.request.servlet.ServletUtil;

import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Tiles LocaleResolver adapter that delegates to a Spring
 * {@link org.springframework.web.servlet.LocaleResolver}, exposing the
 * DispatcherServlet-managed locale.
 *
 * <p>This adapter gets automatically registered by {@link TilesConfigurer}.
 *
 * @author Nicolas Le Bas
 * @since 3.2
 * @see org.apache.tiles.definition.UrlDefinitionsFactory#LOCALE_RESOLVER_IMPL_PROPERTY
 */
public class SpringLocaleResolver extends DefaultLocaleResolver {

	@Override
	public Locale resolveLocale(Request request) {
		try {
			HttpServletRequest servletRequest = ServletUtil.getServletRequest(request).getRequest();
			if (servletRequest != null) {
				return RequestContextUtils.getLocale(servletRequest);
			}
		}
		catch (NotAServletEnvironmentException e) {
			// Ignore
		}
		return super.resolveLocale(request);
	}

}
