/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.view;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSource;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.support.JstlUtils;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Specialization of {@link InternalResourceView} for JSTL pages,
 * i.e. JSP pages that use the JSP Standard Tag Library.
 *
 * <p>Exposes JSTL-specific request attributes specifying locale
 * and resource bundle for JSTL's formatting and message tags,
 * using Spring's locale and {@link org.springframework.context.MessageSource}.
 *
 * <p>Typical usage with {@link InternalResourceViewResolver} would look as follows,
 * from the perspective of the DispatcherServlet context definition:
 *
 * <pre class="code">
 * &lt;bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"&gt;
 *   &lt;property name="viewClass" value="org.springframework.web.servlet.view.JstlView"/&gt;
 *   &lt;property name="prefix" value="/WEB-INF/jsp/"/&gt;
 *   &lt;property name="suffix" value=".jsp"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource"&gt;
 *   &lt;property name="basename" value="messages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Every view name returned from a handler will be translated to a JSP
 * resource (for example: "myView" -> "/WEB-INF/jsp/myView.jsp"), using
 * this view class to enable explicit JSTL support.
 *
 * <p>The specified MessageSource loads messages from "messages.properties" etc
 * files in the class path. This will automatically be exposed to views as
 * JSTL localization context, which the JSTL fmt tags (message etc) will use.
 * Consider using Spring's ReloadableResourceBundleMessageSource instead of
 * the standard ResourceBundleMessageSource for more sophistication.
 * Of course, any other Spring components can share the same MessageSource.
 *
 * <p>This is a separate class mainly to avoid JSTL dependencies in
 * {@link InternalResourceView} itself. JSTL has not been part of standard
 * J2EE up until J2EE 1.4, so we can't assume the JSTL API jar to be
 * available on the class path.
 *
 * <p>Hint: Set the {@link #setExposeContextBeansAsAttributes} flag to "true"
 * in order to make all Spring beans in the application context accessible
 * within JSTL expressions (e.g. in a {@code c:out} value expression).
 * This will also make all such beans accessible in plain {@code ${...}}
 * expressions in a JSP 2.0 page.
 *
 * @author Juergen Hoeller
 * @since 27.02.2003
 * @see org.springframework.web.servlet.support.JstlUtils#exposeLocalizationContext
 * @see InternalResourceViewResolver
 * @see org.springframework.context.support.ResourceBundleMessageSource
 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
 */
public class JstlView extends InternalResourceView {

	@Nullable
	private MessageSource messageSource;


	/**
	 * Constructor for use as a bean.
	 * @see #setUrl
	 */
	public JstlView() {
	}

	/**
	 * Create a new JstlView with the given URL.
	 * @param url the URL to forward to
	 */
	public JstlView(String url) {
		super(url);
	}

	/**
	 * Create a new JstlView with the given URL.
	 * @param url the URL to forward to
	 * @param messageSource the MessageSource to expose to JSTL tags
	 * (will be wrapped with a JSTL-aware MessageSource that is aware of JSTL's
	 * {@code javax.servlet.jsp.jstl.fmt.localizationContext} context-param)
	 * @see JstlUtils#getJstlAwareMessageSource
	 */
	public JstlView(String url, MessageSource messageSource) {
		this(url);
		this.messageSource = messageSource;
	}


	/**
	 * Wraps the MessageSource with a JSTL-aware MessageSource that is aware
	 * of JSTL's {@code javax.servlet.jsp.jstl.fmt.localizationContext}
	 * context-param.
	 * @see JstlUtils#getJstlAwareMessageSource
	 */
	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (this.messageSource != null) {
			this.messageSource = JstlUtils.getJstlAwareMessageSource(servletContext, this.messageSource);
		}
		super.initServletContext(servletContext);
	}

	/**
	 * Exposes a JSTL LocalizationContext for Spring's locale and MessageSource.
	 * @see JstlUtils#exposeLocalizationContext
	 */
	@Override
	protected void exposeHelpers(HttpServletRequest request) throws Exception {
		if (this.messageSource != null) {
			JstlUtils.exposeLocalizationContext(request, this.messageSource);
		}
		else {
			JstlUtils.exposeLocalizationContext(new RequestContext(request, getServletContext()));
		}
	}

}
