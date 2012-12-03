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

package org.springframework.test.context.web;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * TODO [SPR-9864] Document AbstractGenericWebContextLoader.
 *
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class AbstractGenericWebContextLoader extends AbstractContextLoader {

	private static final Log logger = LogFactory.getLog(AbstractGenericWebContextLoader.class);


	// --- SmartContextLoader -----------------------------------------------

	/**
	 * TODO [SPR-9864] Document overridden loadContext(MergedContextConfiguration).
	 *
	 * @see org.springframework.test.context.SmartContextLoader#loadContext(org.springframework.test.context.MergedContextConfiguration)
	 */
	public final ConfigurableApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {

		if (!(mergedConfig instanceof WebMergedContextConfiguration)) {
			throw new IllegalArgumentException(String.format(
				"Cannot load WebApplicationContext from non-web merged context configuration %s. "
						+ "Consider annotating your test class with @WebAppConfiguration.", mergedConfig));
		}
		WebMergedContextConfiguration webMergedConfig = (WebMergedContextConfiguration) mergedConfig;

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Loading WebApplicationContext for merged context configuration %s.",
				webMergedConfig));
		}

		GenericWebApplicationContext context = new GenericWebApplicationContext();
		configureWebResources(context, webMergedConfig);
		prepareContext(context, webMergedConfig);
		customizeBeanFactory(context.getDefaultListableBeanFactory(), webMergedConfig);
		loadBeanDefinitions(context, webMergedConfig);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context, webMergedConfig);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	/**
	 * TODO [SPR-9864] Document configureWebResources().
	 */
	protected void configureWebResources(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {

		String resourceBasePath = webMergedConfig.getResourceBasePath();
		ResourceLoader resourceLoader = resourceBasePath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX) ? new DefaultResourceLoader()
				: new FileSystemResourceLoader();

		ServletContext servletContext = new MockServletContext(resourceBasePath, resourceLoader);
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		context.setServletContext(servletContext);
	}

	/**
	 * TODO [SPR-9864] Document customizeBeanFactory().
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory,
			WebMergedContextConfiguration webMergedConfig) {
	}

	/**
	 * TODO [SPR-9864] Document loadBeanDefinitions().
	 */
	protected abstract void loadBeanDefinitions(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig);

	/**
	 * TODO [SPR-9864] Document customizeContext().
	 */
	protected void customizeContext(GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {
	}

	// --- ContextLoader -------------------------------------------------------

	/**
	 * TODO [SPR-9864] Document overridden loadContext(String...).
	 *
	 * @see org.springframework.test.context.ContextLoader#loadContext(java.lang.String[])
	 */
	public final ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException(
			"AbstractGenericWebContextLoader does not support the loadContext(String... locations) method");
	}

}
