/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a {@code view-controller} element to register
 * a {@link ParameterizableViewController}.  Will also register a {@link SimpleUrlHandlerMapping} for view controllers.
 * 
 * @author Keith Donald
 * @since 3.0
 */
class ViewControllerBeanDefinitionParser implements BeanDefinitionParser {

	private String handlerAdapterBeanName;
	
	private String handlerMappingBeanName;
	
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		if (this.handlerAdapterBeanName == null) {
			RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(SimpleControllerHandlerAdapter.class);
			handlerAdapterDef.setSource(source);
			handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			this.handlerAdapterBeanName = parserContext.getReaderContext().registerWithGeneratedName(handlerAdapterDef);
			parserContext.registerComponent(new BeanComponentDefinition(handlerAdapterDef, handlerAdapterBeanName));
		}
		RootBeanDefinition handlerMappingDef;
		if (this.handlerMappingBeanName == null) {
			handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
			handlerMappingDef.setSource(source);
			handlerMappingDef.getPropertyValues().add("order", "1");
			handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			this.handlerMappingBeanName = parserContext.getReaderContext().registerWithGeneratedName(handlerMappingDef);
			parserContext.registerComponent(new BeanComponentDefinition(handlerMappingDef, handlerMappingBeanName));
		} else {
			handlerMappingDef = (RootBeanDefinition) parserContext.getReaderContext().getRegistry().getBeanDefinition(this.handlerMappingBeanName);
		}
		RootBeanDefinition viewControllerDef = new RootBeanDefinition(ParameterizableViewController.class);
		viewControllerDef.setSource(source);
		if (element.hasAttribute("view-name")) {
			viewControllerDef.getPropertyValues().add("viewName", element.getAttribute("view-name"));			
		}
		Map<String, BeanDefinition> urlMap;
		if (handlerMappingDef.getPropertyValues().contains("urlMap")) {
			urlMap = (Map<String, BeanDefinition>) handlerMappingDef.getPropertyValues().getPropertyValue("urlMap").getValue();
		} else {
			urlMap = new ManagedMap<String, BeanDefinition>();
			handlerMappingDef.getPropertyValues().add("urlMap", urlMap);			
		}
		urlMap.put(element.getAttribute("path"), viewControllerDef);
		return null;
	}
	
}
