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

package org.springframework.web.servlet.view.tiles2;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.jsp.context.JspTilesRequestContext;
import org.apache.tiles.locale.impl.DefaultLocaleResolver;
import org.apache.tiles.servlet.context.ServletTilesRequestContext;

import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Tiles LocaleResolver adapter that delegates to a Spring
 * {@link org.springframework.web.servlet.LocaleResolver},
 * exposing the DispatcherServlet-managed locale.
 *
 * <p>This adapter gets automatically registered by {@link TilesConfigurer}.
 * If you are using standard Tiles bootstrap, specify the name of this class
 * as value for the init-param "org.apache.tiles.locale.LocaleResolver".
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.apache.tiles.definition.UrlDefinitionsFactory#LOCALE_RESOLVER_IMPL_PROPERTY
 */
public class SpringLocaleResolver extends DefaultLocaleResolver {

	@Override
	public Locale resolveLocale(TilesRequestContext context) {
		if (context instanceof JspTilesRequestContext) {
			PageContext pc = ((JspTilesRequestContext) context).getPageContext();
			return RequestContextUtils.getLocale((HttpServletRequest) pc.getRequest());
		}
		else if (context instanceof ServletTilesRequestContext) {
			HttpServletRequest request = ((ServletTilesRequestContext) context).getRequest();
			if (request != null) {
				return RequestContextUtils.getLocale(request);
			}
		}
		return super.resolveLocale(context);
	}

}
