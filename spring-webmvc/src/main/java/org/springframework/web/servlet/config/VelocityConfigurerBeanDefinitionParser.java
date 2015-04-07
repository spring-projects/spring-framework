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

package org.springframework.web.servlet.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;


/**
 * Parse the <mvc:velocity-configurer> MVC namespace element and register an
 * VelocityConfigurer bean
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class VelocityConfigurerBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	public static final String BEAN_NAME = "mvcVelocityConfigurer";

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return BEAN_NAME;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.web.servlet.view.velocity.VelocityConfigurer";
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return attributeName.equals("resource-loader-path");
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element) {
		if (!builder.getBeanDefinition().hasAttribute("resourceLoaderPath")) {
			builder.getBeanDefinition().setAttribute("resourceLoaderPath", "/WEB-INF/");
		}
	}
}
