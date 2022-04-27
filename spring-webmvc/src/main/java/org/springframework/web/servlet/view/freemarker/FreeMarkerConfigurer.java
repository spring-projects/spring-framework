/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.view.freemarker;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

/**
 * JavaBean to configure FreeMarker for web usage, via the "configLocation"
 * and/or "freemarkerSettings" and/or "templateLoaderPath" properties.
 * The simplest way to use this class is to specify just a "templateLoaderPath";
 * you do not need any further configuration then.
 *
 * <pre class="code">
 * &lt;bean id="freemarkerConfig" class="org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer"&gt;
 *   &lt;property name="templateLoaderPath"&gt;&lt;value&gt;/WEB-INF/freemarker/&lt;/value&gt;&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * This bean must be included in the application context of any application
 * using Spring's FreeMarkerView for web MVC. It exists purely to configure FreeMarker.
 * It is not meant to be referenced by application components but just internally
 * by FreeMarkerView. Implements FreeMarkerConfig to be found by FreeMarkerView without
 * depending on the bean name of the configurer. Each DispatcherServlet can define its
 * own FreeMarkerConfigurer if desired.
 *
 * <p>Note that you can also refer to a preconfigured FreeMarker Configuration
 * instance, for example one set up by FreeMarkerConfigurationFactoryBean, via
 * the "configuration" property. This allows to share a FreeMarker Configuration
 * for web and email usage, for example.
 *
 * <p>This configurer registers a template loader for this package, allowing to
 * reference the "spring.ftl" macro library contained in this package:
 *
 * <pre class="code">
 * &lt;#import "/spring.ftl" as spring/&gt;
 * &lt;@spring.bind "person.age"/&gt;
 * age is ${spring.status.value}</pre>
 *
 * Note: Spring's FreeMarker support requires FreeMarker 2.3 or higher.
 *
 * @author Darren Davison
 * @author Rob Harrop
 * @since 03.03.2004
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setTemplateLoaderPath
 * @see #setConfiguration
 * @see org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean
 * @see FreeMarkerView
 */
public class FreeMarkerConfigurer extends FreeMarkerConfigurationFactory
		implements FreeMarkerConfig, InitializingBean, ResourceLoaderAware, ServletContextAware {

	@Nullable
	private Configuration configuration;

	@Nullable
	private TaglibFactory taglibFactory;


	/**
	 * Set a preconfigured Configuration to use for the FreeMarker web config, e.g. a
	 * shared one for web and email usage, set up via FreeMarkerConfigurationFactoryBean.
	 * If this is not set, FreeMarkerConfigurationFactory's properties (inherited by
	 * this class) have to be specified.
	 * @see org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Initialize the {@link TaglibFactory} for the given ServletContext.
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.taglibFactory = new TaglibFactory(servletContext);
	}


	/**
	 * Initialize FreeMarkerConfigurationFactory's Configuration
	 * if not overridden by a preconfigured FreeMarker Configuration.
	 * <p>Sets up a ClassTemplateLoader to use for loading Spring macros.
	 * @see #createConfiguration
	 * @see #setConfiguration
	 */
	@Override
	public void afterPropertiesSet() throws IOException, TemplateException {
		if (this.configuration == null) {
			this.configuration = createConfiguration();
		}
	}

	/**
	 * This implementation registers an additional ClassTemplateLoader
	 * for the Spring-provided macros, added to the end of the list.
	 */
	@Override
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
		templateLoaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class, ""));
	}


	/**
	 * Return the Configuration object wrapped by this bean.
	 */
	@Override
	public Configuration getConfiguration() {
		Assert.state(this.configuration != null, "No Configuration available");
		return this.configuration;
	}

	/**
	 * Return the TaglibFactory object wrapped by this bean.
	 */
	@Override
	public TaglibFactory getTaglibFactory() {
		Assert.state(this.taglibFactory != null, "No TaglibFactory available");
		return this.taglibFactory;
	}

}
