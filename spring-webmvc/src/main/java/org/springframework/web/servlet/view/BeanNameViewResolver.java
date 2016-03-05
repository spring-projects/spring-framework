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

package org.springframework.web.servlet.view;

import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import org.jfree.util.Log;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * A simple implementation of {@link org.springframework.web.servlet.ViewResolver} that
 * interprets a view name as a bean name in the current application context, i.e.
 * typically in the XML file of the executing {@code DispatcherServlet}.
 *
 * <p>
 * This resolver can be handy for small applications, keeping all definitions ranging from
 * controllers to views in the same place. For larger applications,
 * {@link XmlViewResolver} will be the better choice, as it separates the XML view bean
 * definitions into a dedicated views file.
 *
 * <p>
 * Note: Neither this {@code ViewResolver} nor {@link XmlViewResolver} supports
 * internationalization. Consider {@link ResourceBundleViewResolver} if you need to apply
 * different view resources per locale.
 *
 * <p>
 * Note: This {@code ViewResolver} implements the {@link Ordered} interface in order to
 * allow for flexible participation in {@code ViewResolver} chaining. For example, some
 * special views could be defined via this {@code ViewResolver} (giving it 0 as "order"
 * value), while all remaining views could be resolved by a {@link UrlBasedViewResolver}.
 *
 * @author Juergen Hoeller
 * @since 18.06.2003
 * @see XmlViewResolver
 * @see ResourceBundleViewResolver
 * @see UrlBasedViewResolver
 */
public class BeanNameViewResolver extends WebApplicationObjectSupport
		implements ViewResolver, Ordered {

	private int order = Integer.MAX_VALUE; // default: same as non-Ordered

	/** Default if no other location is supplied */
	public final static String DEFAULT_LOCATION = "/WEB-INF/views.xml";

	private Resource location;

	private Class<?> configuration;

	private Stack<ApplicationContext> cxts = new Stack<ApplicationContext>();

	private boolean init = false;

	public void setConfiguration(Class<?> configuration) {
		this.configuration = configuration;
	}

	public void setLocation(Resource location) {
		this.location = location;
	}

	public Resource getLocation() {
		return location;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public View resolveViewName(String viewName, Locale locale) throws BeansException {

		if (!init) {
			initFactory();
		}

		for (ApplicationContext context : cxts) {
			if (!context.containsBean(viewName)) {
				continue;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Matching bean found for view name '" + viewName
						+ "' with the context name " + context.getDisplayName());
			}
			if (!context.isTypeMatch(viewName, View.class)) {
				continue;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Found matching bean for view name '" + viewName
						+ "' - implement View with the context name "
						+ context.getDisplayName());
			}
			return context.getBean(viewName, View.class);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Cannot find the matching bean for view name " + viewName
					+ " within the context " + cxts.toString());
		}

		return null;
	}

	/**
	 * Initialize the view bean factory from the XML file Or @Configuration. The default
	 * load method is from the XML, If want to load from @Configuration Synchronized
	 * because of access by parallel threads.
	 * 
	 * @throws BeansException in case of initialization errors
	 */
	protected synchronized BeanFactory initFactory() throws BeansException {
		cxts.push(getApplicationContext());

		// Create child ApplicationContext for views.
		GenericWebApplicationContext factory = new GenericWebApplicationContext();
		factory.setParent(getApplicationContext());
		factory.setServletContext(getServletContext());

		// load the bean from XML if the location is not null
		if (this.location != null) {
			cxts.push(factory);
			// Load XML resource with context-aware entity resolver.
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
			reader.setEnvironment(getApplicationContext().getEnvironment());
			reader.setEntityResolver(new ResourceEntityResolver(getApplicationContext()));
			int numOfBeans = reader.loadBeanDefinitions(this.location);
			if (Log.isDebugEnabled()) {
				Log.debug("Context name " + factory.getApplicationName()
						+ " resolve the number of beans " + numOfBeans);
			}
		}

		// load the bean from Annotation if the annotation class is not null
		if (this.configuration != null) {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.register(configuration);
//			context.setParent(factory);
			cxts.push(context);
			context.refresh();
			Map<String, Object> beans = context.getBeansWithAnnotation(
					Configuration.class);
			if (Log.isDebugEnabled()) {
				Log.debug("Context name " + factory.getApplicationName()
						+ " resolve the number of beans " + beans.size());
			}
		}

		factory.refresh();

		this.init = true;

		return factory;
	}

}
