/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * A simple implementation of {@link org.springframework.web.servlet.ViewResolver}
 * that interprets a view name as a bean name in an application context (generally
 * the current application context or a pre-defined one passed in through the
 * constructor), i.e. typically in the XML file of the executing {@code DispatcherServlet}
 * or in a corresponding configuration class.
 *
 * <p>Note: This {@code ViewResolver} implements the {@link Ordered} interface
 * in order to allow for flexible participation in {@code ViewResolver} chaining.
 * For example, some special views could be defined via this {@code ViewResolver}
 * (giving it 0 as "order" value), while all remaining views could be resolved by
 * a {@link UrlBasedViewResolver}.
 *
 * @author Juergen Hoeller
 * @author Marten Deinum
 * @since 18.06.2003
 * @see UrlBasedViewResolver
 */
public class BeanNameViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered, InitializingBean, DisposableBean {

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	@Nullable
	private ApplicationContext cachedContext;
	@Nullable
	private final ApplicationContext providedContext;

	public BeanNameViewResolver() {
		this.providedContext = null;
	}

	public BeanNameViewResolver(ApplicationContext context) {
		this.providedContext=context;
	}

	/**
	 * Specify the order value for this ViewResolver bean.
	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws BeansException {
		BeanFactory factory = initFactory();
		if (!factory.containsBean(viewName)) {
			// Allow for ViewResolver chaining...
			return null;
		}
		if (!factory.isTypeMatch(viewName, View.class)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found bean named '" + viewName + "' but it does not implement View");
			}
			// Since we're looking into the general ApplicationContext here,
			// let's accept this as a non-match and allow for chaining as well...
			return null;
		}
		return factory.getBean(viewName, View.class);
	}

	/**
	 * Initialize the view bean factory.
	 * Synchronized because of access by parallel threads.
	 * @throws BeansException in case of initialization errors
	 */
	protected synchronized BeanFactory initFactory() throws BeansException {
		if (this.cachedContext != null) {
			return this.cachedContext;
		}

		ApplicationContext applicationContext = obtainApplicationContext();
		if (this.providedContext == null) {
			this.cachedContext = applicationContext;
		}
		else {
			if (this.providedContext instanceof ConfigurableApplicationContext cac) {
				cac.setParent(applicationContext);
				if (this.providedContext instanceof ConfigurableWebApplicationContext wac) {
					wac.setServletContext(getServletContext());
				}
			}
			this.cachedContext = this.providedContext;
		}
		return this.cachedContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initFactory();
	}

	@Override
	public void destroy() throws Exception {
		if (this.providedContext instanceof ConfigurableApplicationContext cac) {
			cac.close();
		}
	}
}
