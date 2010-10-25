/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.EnvironmentAwarePropertyPlaceholderConfigurer;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;context:property-placeholder/&gt; element.
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @since 2.5
 */
class PropertyPlaceholderBeanDefinitionParser extends AbstractPropertyLoadingBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		// as of Spring 3.1, the default for system-properties-mode is DELEGATE,
		// meaning that the attribute should be disregarded entirely, instead
		// deferring to the order of PropertySource objects in the enclosing
		// application context's Environment object
		if (!"DELEGATE".equals(element.getAttribute("system-properties-mode"))) {
			return PropertyPlaceholderConfigurer.class;
		}

		return EnvironmentAwarePropertyPlaceholderConfigurer.class;
	}

	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
	
		super.doParse(element, builder);

		builder.addPropertyValue("ignoreUnresolvablePlaceholders",
				Boolean.valueOf(element.getAttribute("ignore-unresolvable")));

		if (!"DELEGATE".equals(element.getAttribute("system-properties-mode"))) {
			String systemPropertiesModeName = element.getAttribute("system-properties-mode");
			if (StringUtils.hasLength(systemPropertiesModeName)) {
				builder.addPropertyValue("systemPropertiesModeName", "SYSTEM_PROPERTIES_MODE_"+systemPropertiesModeName);
			}
		}

	}

}
