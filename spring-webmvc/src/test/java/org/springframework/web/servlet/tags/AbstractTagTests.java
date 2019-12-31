/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.tags;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.SimpleWebApplicationContext;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockPageContext;
import org.springframework.web.testfixture.servlet.MockServletContext;

/**
 * Abstract base class for testing tags; provides {@link #createPageContext()}.
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public abstract class AbstractTagTests {

	protected MockPageContext createPageContext() {
		MockServletContext sc = new MockServletContext();
		SimpleWebApplicationContext wac = new SimpleWebApplicationContext();
		wac.setServletContext(sc);
		wac.setNamespace("test");
		wac.refresh();

		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();
		if (inDispatcherServlet()) {
			request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
			LocaleResolver lr = new AcceptHeaderLocaleResolver();
			request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, lr);
			ThemeResolver tr = new FixedThemeResolver();
			request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, tr);
			request.setAttribute(DispatcherServlet.THEME_SOURCE_ATTRIBUTE, wac);
		}
		else {
			sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		}

		return new MockPageContext(sc, request, response);
	}

	protected boolean inDispatcherServlet() {
		return true;
	}

}
