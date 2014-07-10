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

import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.StatusController;

/**
 * {@link BeanDefinitionParser} that parses a
 * {@code status-controller} element to register a {@link StatusController}.
 * Will also register a {@link SimpleUrlHandlerMapping} for status controllers.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
class StatusControllerBeanDefinitionParser implements BeanDefinitionParser {

	private static final String HANDLER_MAPPING_BEAN_NAME =
		"org.springframework.web.servlet.config.statusControllerHandlerMapping";


	@Override
	@SuppressWarnings("unchecked")
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		// Register SimpleUrlHandlerMapping for status controllers
		BeanDefinition handlerMappingDef = registerHandlerMapping(parserContext, source);

		// Ensure BeanNameUrlHandlerMapping (SPR-8289) and default HandlerAdapters are not "turned off"
		MvcNamespaceUtils.registerDefaultComponents(parserContext, source);

		// Create status controller bean definition
		RootBeanDefinition statusControllerDef = new RootBeanDefinition(StatusController.class);
		statusControllerDef.setSource(source);
		HttpStatus statusCode = HttpStatus.valueOf(Integer.valueOf(element.getAttribute("status-code")));
		statusControllerDef.getConstructorArgumentValues().addIndexedArgumentValue(0, statusCode);
		if (element.hasAttribute("view-name")) {
			statusControllerDef.getPropertyValues().add("viewName", element.getAttribute("view-name"));
		}
		if (element.hasAttribute("redirect-path")) {
			statusControllerDef.getPropertyValues().add("redirectPath", element.getAttribute("redirect-path"));
		}
		if (element.hasAttribute("use-query-string")) {
			statusControllerDef.getPropertyValues().add("useQueryString", element.getAttribute("use-query-string"));
		}
		if (element.hasAttribute("reason")) {
			statusControllerDef.getPropertyValues().add("reason", element.getAttribute("reason"));
		}

		Map<String, BeanDefinition> urlMap;
		if (handlerMappingDef.getPropertyValues().contains("urlMap")) {
			urlMap = (Map<String, BeanDefinition>) handlerMappingDef.getPropertyValues().getPropertyValue("urlMap").getValue();
		}
		else {
			urlMap = new ManagedMap<String, BeanDefinition>();
			handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
		}
		urlMap.put(element.getAttribute("path"), statusControllerDef);

		return null;
	}

	private BeanDefinition registerHandlerMapping(ParserContext parserContext, Object source) {
		if (!parserContext.getRegistry().containsBeanDefinition(HANDLER_MAPPING_BEAN_NAME)) {
			RuntimeBeanReference pathMatcherRef = MvcNamespaceUtils.registerPathMatcher(null, parserContext, source);
			RuntimeBeanReference pathHelperRef = MvcNamespaceUtils.registerUrlPathHelper(null, parserContext, source);

			RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
			handlerMappingDef.setSource(source);
			handlerMappingDef.getPropertyValues().add("order", "-1");
			handlerMappingDef.getPropertyValues().add("pathMatcher", pathMatcherRef).add("urlPathHelper", pathHelperRef);
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
