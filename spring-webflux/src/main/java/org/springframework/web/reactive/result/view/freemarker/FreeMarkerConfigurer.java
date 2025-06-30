/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.result.view.freemarker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.util.Assert;

/**
 * Configures FreeMarker for web usage via the "configLocation",
 * "freemarkerSettings", or "templateLoaderPath" properties.
 *
 * <p>The simplest way to use this class is to specify just a "templateLoaderPath"
 * (for example, "classpath:templates"); you do not need any further configuration then.
 *
 * <p>This bean must be included in the application context of any application
 * using {@link FreeMarkerView}. It exists purely to configure FreeMarker.
 * It is not meant to be referenced by application components but just internally
 * by {@code FreeMarkerView}. Implements {@link FreeMarkerConfig} to be found by
 * {@code FreeMarkerView} without depending on the bean name of the configurer.
 *
 * <p>Note that you can also refer to a pre-configured FreeMarker {@code Configuration}
 * instance via the "configuration" property. This allows to share a FreeMarker
 * {@code Configuration} for web and email usage for example.
 *
 * <p>This configurer registers a template loader for this package, allowing to
 * reference the "spring.ftl" macro library contained in this package:
 *
 * <pre class="code">
 * &lt;#import "/spring.ftl" as spring/&gt;
 * &lt;@spring.bind "person.age"/&gt;
 * age is ${spring.status.value}</pre>
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3.33 or higher.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FreeMarkerConfigurer extends FreeMarkerConfigurationFactory
		implements FreeMarkerConfig, InitializingBean, ResourceLoaderAware {

	private @Nullable Configuration configuration;


	public FreeMarkerConfigurer() {
		setDefaultCharset(StandardCharsets.UTF_8);
	}


	/**
	 * Set a preconfigured {@link Configuration} to use for the FreeMarker web
	 * config &mdash; for example, a shared one for web and email usage.
	 * <p>If this is not set, FreeMarkerConfigurationFactory's properties (inherited
	 * by this class) have to be specified.
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}


	/**
	 * Initialize FreeMarkerConfigurationFactory's {@link Configuration}
	 * if not overridden by a pre-configured FreeMarker {@link Configuration}.
	 * <p>Indirectly sets up a {@link ClassTemplateLoader} to use for loading
	 * Spring macros.
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
	 * This implementation registers an additional {@link ClassTemplateLoader}
	 * for the Spring-provided macros, added to the end of the list.
	 */
	@Override
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
		templateLoaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class, ""));
	}


	/**
	 * Return the {@link Configuration} object wrapped by this bean.
	 */
	@Override
	public Configuration getConfiguration() {
		Assert.state(this.configuration != null, "No Configuration available");
		return this.configuration;
	}

}
