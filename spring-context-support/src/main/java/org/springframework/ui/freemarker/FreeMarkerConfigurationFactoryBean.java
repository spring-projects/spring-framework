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

package org.springframework.ui.freemarker;

import java.io.IOException;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;

/**
 * Factory bean that creates a FreeMarker {@link Configuration} and provides it
 * as a bean reference.
 *
 * <p>This bean is intended for any kind of usage of FreeMarker in application
 * code &mdash; for example, for generating email content. For web views,
 * {@code FreeMarkerConfigurer} is used to set up a {@link FreeMarkerConfigurationFactory}.
 *
 * <p>The simplest way to use this class is to specify just a "templateLoaderPath";
 * you do not need any further configuration then. For example, in a web
 * application context:
 *
 * <pre class="code"> &lt;bean id="freemarkerConfiguration" class="org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean"&gt;
 *   &lt;property name="templateLoaderPath" value="/WEB-INF/freemarker/"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>See the {@link FreeMarkerConfigurationFactory} base class for configuration
 * details.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3.33 or higher.
 *
 * @author Darren Davison
 * @since 03.03.2004
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setTemplateLoaderPath
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
 */
public class FreeMarkerConfigurationFactoryBean extends FreeMarkerConfigurationFactory
		implements FactoryBean<Configuration>, InitializingBean, ResourceLoaderAware {

	private @Nullable Configuration configuration;


	@Override
	public void afterPropertiesSet() throws IOException, TemplateException {
		this.configuration = createConfiguration();
	}


	@Override
	public @Nullable Configuration getObject() {
		return this.configuration;
	}

	@Override
	public Class<? extends Configuration> getObjectType() {
		return Configuration.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
