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

package org.springframework.web.servlet.config;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a
 * {@code view-controller} element to register a {@link ParameterizableViewController}.
 * Will also register a {@link SimpleUrlHandlerMapping} for view controllers.
 *
 * @author Keith Donald
 * @author Christian Dupuis
 * @since 3.0
 */
class ViewControllerBeanDefinitionParser implements BeanDefinitionParser {

	private static final String HANDLER_MAPPING_BEAN_NAME =
		"org.springframework.web.servlet.config.viewControllerHandlerMapping";


	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		// Register SimpleUrlHandlerMapping for view controllers
		BeanDefinition handlerMappingDef = registerHandlerMapping(parserContext, source);

		// Ensure BeanNameUrlHandlerMapping (SPR-8289) and default HandlerAdapters are not "turned off"
		MvcNamespaceUtils.registerDefaultComponents(parserContext, source);

		// Create view controller bean definition
		RootBeanDefinition viewControllerDef = new RootBeanDefinition(ParameterizableViewController.class);
		viewControllerDef.setSource(source);
		if (element.hasAttribute("view-name")) {
			viewControllerDef.getPropertyValues().add("viewName", element.getAttribute("view-name"));
		}
		Map<String, BeanDefinition> urlMap;
		if (handlerMappingDef.getPropertyValues().contains("urlMap")) {
			urlMap = (Map<String, BeanDefinition>) handlerMappingDef.getPropertyValues().getPropertyValue("urlMap").getValue();
		}
		else {
			urlMap = new ManagedMap<String, BeanDefinition>();
			handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
		}
		urlMap.put(element.getAttribute("path"), viewControllerDef);

		return null;
	}

	private BeanDefinition registerHandlerMapping(ParserContext parserContext, Object source) {
		if (!parserContext.getRegistry().containsBeanDefinition(HANDLER_MAPPING_BEAN_NAME)) {
			RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
			handlerMappingDef.setSource(source);
			handlerMappingDef.getPropertyValues().add("order", "1");
			handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			parserContext.getRegistry().registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME, handlerMappingDef);
			parserContext.registerComponent(new BeanComponentDefinition(handlerMappingDef, HANDLER_MAPPING_BEAN_NAME));
			return handlerMappingDef;
		}
		else {
			return parserContext.getRegistry().getBeanDefinition(HANDLER_MAPPING_BEAN_NAME);
		}

	}

}
