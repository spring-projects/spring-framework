/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A {@link ViewResolverComposite} that delegates to a list of other {@link ViewResolver}s.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class ViewResolverComposite implements ApplicationContextAware, ServletContextAware, ViewResolver, Ordered {

	private List<ViewResolver> viewResolvers;

	private int order = Ordered.LOWEST_PRECEDENCE;

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the list of view viewResolvers to delegate to.
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	/**
	 * Return the list of view viewResolvers to delegate to.
	 */
	public List<ViewResolver> getViewResolvers() {
		return Collections.unmodifiableList(viewResolvers);
	}

	@Override
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		if (viewResolvers != null) {
			for (ViewResolver viewResolver : viewResolvers) {
				View v = viewResolver.resolveViewName(viewName, locale);
				if (v != null) {
					return v;
				}
			}
		}

		return null;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (viewResolvers != null) {
			for (ViewResolver viewResolver : viewResolvers) {
				if(viewResolver instanceof ApplicationContextAware) {
					((ApplicationContextAware)viewResolver).setApplicationContext(applicationContext);
				}
			}
		}
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (viewResolvers != null) {
			for (ViewResolver viewResolver : viewResolvers) {
				if(viewResolver instanceof ServletContextAware) {
					((ServletContextAware)viewResolver).setServletContext(servletContext);
				}
			}
		}
	}
}
