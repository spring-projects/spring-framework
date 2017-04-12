/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.view.groovy;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import groovy.text.markup.TemplateResolver;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * An extension of Groovy's {@link groovy.text.markup.TemplateConfiguration} and
 * an implementation of Spring MVC's {@link GroovyMarkupConfig} for creating
 * a {@code MarkupTemplateEngine} for use in a web application. The most basic
 * way to configure this class is to set the "resourceLoaderPath". For example:
 *
 * <pre class="code">
 *
 * // Add the following to an &#64;Configuration class
 *
 * &#64;Bean
 * public GroovyMarkupConfig groovyMarkupConfigurer() {
 *     GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
 *     configurer.setResourceLoaderPath("classpath:/WEB-INF/groovymarkup/");
 *     return configurer;
 * }
 * </pre>
 *
 * By default this bean will create a {@link MarkupTemplateEngine} with:
 * <ul>
 * <li>a parent ClassLoader for loading Groovy templates with their references
 * <li>the default configuration in the base class {@link TemplateConfiguration}
 * <li>a {@link groovy.text.markup.TemplateResolver} for resolving template files
 * </ul>
 *
 * You can provide the {@link MarkupTemplateEngine} instance directly to this bean
 * in which case all other properties will not be effectively ignored.
 *
 * <p>This bean must be included in the application context of any application
 * using the Spring MVC {@link GroovyMarkupView} for rendering. It exists purely
 * for the purpose of configuring Groovy's Markup templates. It is not meant to be
 * referenced by application components directly. It implements GroovyMarkupConfig
 * to be found by GroovyMarkupView without depending on a bean name. Each
 * DispatcherServlet can define its own GroovyMarkupConfigurer if desired.
 *
 * <p>Note that resource caching is enabled by default in {@link MarkupTemplateEngine}.
 * Use the {@link #setCacheTemplates(boolean)} to configure that as necessary.

 * <p>Spring's Groovy Markup template support requires Groovy 2.3.1 or higher.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see GroovyMarkupView
 * @see <a href="http://groovy-lang.org/templating.html#_the_markuptemplateengine">
 *     Groovy Markup Template engine documentation</a>
 */
public class GroovyMarkupConfigurer extends TemplateConfiguration
		implements GroovyMarkupConfig, ApplicationContextAware, InitializingBean {

	private String resourceLoaderPath = "classpath:";

	private MarkupTemplateEngine templateEngine;

	private ApplicationContext applicationContext;


	/**
	 * Set the Groovy Markup Template resource loader path(s) via a Spring resource
	 * location. Accepts multiple locations as a comma-separated list of paths.
	 * Standard URLs like "file:" and "classpath:" and pseudo URLs are supported
	 * as understood by Spring's {@link org.springframework.core.io.ResourceLoader}.
	 * Relative paths are allowed when running in an ApplicationContext.
	 *
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

	/**
	 * Set a pre-configured MarkupTemplateEngine to use for the Groovy Markup
	 * Template web configuration.
	 * <p>Note that this engine instance has to be manually configured, since all
	 * other bean properties of this configurer will be ignored.
	 */
	public void setTemplateEngine(MarkupTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	public MarkupTemplateEngine getTemplateEngine() {
		return templateEngine;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * This method should not be used, since the considered Locale for resolving
	 * templates is the Locale for the current HTTP request.
	 */
	@Override
	public void setLocale(Locale locale) {
		super.setLocale(locale);
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.templateEngine == null) {
			this.templateEngine = createTemplateEngine();
		}
	}

	protected MarkupTemplateEngine createTemplateEngine() throws IOException {
		if (this.templateEngine == null) {
			ClassLoader templateClassLoader = createTemplateClassLoader();
			this.templateEngine = new MarkupTemplateEngine(templateClassLoader, this, new LocaleTemplateResolver());
		}
		return this.templateEngine;
	}

	/**
	 * Create a parent ClassLoader for Groovy to use as parent ClassLoader
	 * when loading and compiling templates.
	 */
	protected ClassLoader createTemplateClassLoader() throws IOException {
		String[] paths = StringUtils.commaDelimitedListToStringArray(getResourceLoaderPath());
		List<URL> urls = new ArrayList<>();
		for (String path : paths) {
			Resource[] resources = getApplicationContext().getResources(path);
			if (resources.length > 0) {
				for (Resource resource : resources) {
					if (resource.exists()) {
						urls.add(resource.getURL());
					}
				}
			}
		}
		ClassLoader classLoader = getApplicationContext().getClassLoader();
		return (urls.size() > 0 ? new URLClassLoader(urls.toArray(new URL[urls.size()]), classLoader) : classLoader);
	}

	/**
	 * Resolve a template from the given template path.
	 * <p>The default implementation uses the Locale associated with the current request,
	 * as obtained through {@link org.springframework.context.i18n.LocaleContextHolder LocaleContextHolder},
	 * to find the template file. Effectively the locale configured at the engine level is ignored.
	 * @see LocaleContextHolder
	 * @see #setLocale
	 */
	protected URL resolveTemplate(ClassLoader classLoader, String templatePath) throws IOException {
		MarkupTemplateEngine.TemplateResource resource = MarkupTemplateEngine.TemplateResource.parse(templatePath);
		Locale locale = LocaleContextHolder.getLocale();
		URL url = classLoader.getResource(resource.withLocale(locale.toString().replace("-", "_")).toString());
		if (url == null) {
			url = classLoader.getResource(resource.withLocale(locale.getLanguage()).toString());
		}
		if (url == null) {
			url = classLoader.getResource(resource.withLocale(null).toString());
		}
		if (url == null) {
			throw new IOException("Unable to load template:" + templatePath);
		}
		return url;
	}


	/**
	 * Custom {@link TemplateResolver template resolver} that simply delegates to
	 * {@link #resolveTemplate(ClassLoader, String)}..
	 */
	private class LocaleTemplateResolver implements TemplateResolver {

		private ClassLoader classLoader;

		@Override
		public void configure(ClassLoader templateClassLoader, TemplateConfiguration configuration) {
			this.classLoader = templateClassLoader;
		}

		@Override
		public URL resolveTemplate(String templatePath) throws IOException {
			return GroovyMarkupConfigurer.this.resolveTemplate(this.classLoader, templatePath);
		}
	}

}
