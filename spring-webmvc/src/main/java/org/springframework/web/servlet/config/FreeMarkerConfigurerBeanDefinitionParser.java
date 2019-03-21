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

package org.springframework.web.servlet.config;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parse the <mvc:freemarker-configurer> MVC namespace element and register
 * FreeMarkerConfigurer bean
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class FreeMarkerConfigurerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	public static final String BEAN_NAME = "mvcFreeMarkerConfigurer";


	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer";
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return BEAN_NAME;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "template-loader-path");
		if (!childElements.isEmpty()) {
			List<String> locations = new ArrayList<String>(childElements.size());
			for (Element childElement : childElements) {
				locations.add(childElement.getAttribute("location"));
			}
			if (locations.isEmpty()) {
				locations.add("/WEB-INF/");
			}
			builder.addPropertyValue("templateLoaderPaths", StringUtils.toStringArray(locations));
		}
	}

}
