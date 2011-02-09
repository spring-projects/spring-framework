/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.config;

import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.config.AbstractSpecificationBeanDefinitionParser;
import org.springframework.context.config.FeatureSpecification;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a
 * {@code resources} element. 
 *
 * @author Rossen Stoyanchev
 * @since 3.0.4
 * @see MvcResources
 * @see MvcResourcesExecutor
 */
class ResourcesBeanDefinitionParser extends AbstractSpecificationBeanDefinitionParser {

	/**
	 * Parses the {@code <mvc:resources/>} tag
	 */
	public FeatureSpecification doParse(Element element, ParserContext parserContext) {
		String mapping = element.getAttribute("mapping");
		String[] locations =
			StringUtils.commaDelimitedListToStringArray(element.getAttribute("location"));

		MvcResources spec = new MvcResources(mapping, locations);
		if (element.hasAttribute("cache-period")) {
			spec.cachePeriod(element.getAttribute("cache-period"));
		}
		if (element.hasAttribute("order")) {
			spec.order(element.getAttribute("order"));
		}

		return spec;
	}

}
