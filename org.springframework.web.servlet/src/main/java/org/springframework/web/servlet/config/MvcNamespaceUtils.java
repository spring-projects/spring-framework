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

package org.springframework.web.servlet.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;

/**
 * Convenience methods for MVC namespace handlers.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
abstract class MvcNamespaceUtils {

	private static final String BEAN_NAME_URL_HANDLER_MAPPING = 
		"org.springframework.web.servlet.handler.beanNameUrlHandlerMapping";

	private static final String VIEW_CONTROLLER_HANDLER_ADAPTER = 
		"org.springframework.web.servlet.config.viewControllerHandlerAdapter";

	private static final String HTTP_REQUEST_HANDLER_ADAPTER = 
		"org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter";

	public static void registerDefaultHandlerAdapters(ParserContext parserContext, Object source) {
		registerHttpRequestHandlerAdapter(parserContext, source);
		registerSimpleControllerHandlerAdapter(parserContext, source);
	}

	public static void registerBeanNameUrlHandlerMapping(ParserContext parserContext, Object source) {
		if (!parserContext.getRegistry().containsBeanDefinition(BEAN_NAME_URL_HANDLER_MAPPING)){
			RootBeanDefinition beanNameMappingDef = new RootBeanDefinition(BeanNameUrlHandlerMapping.class);
			beanNameMappingDef.setSource(source);
			beanNameMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			beanNameMappingDef.getPropertyValues().add("order", 2);	// consistent with MvcConfiguration
			parserContext.getRegistry().registerBeanDefinition(BEAN_NAME_URL_HANDLER_MAPPING, beanNameMappingDef);
			parserContext.registerComponent(new BeanComponentDefinition(beanNameMappingDef, BEAN_NAME_URL_HANDLER_MAPPING));
		}
	}

	public static void registerHttpRequestHandlerAdapter(ParserContext parserContext, Object source) {
		if (!parserContext.getRegistry().containsBeanDefinition(HTTP_REQUEST_HANDLER_ADAPTER)) {
			RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(HttpRequestHandlerAdapter.class);
			handlerAdapterDef.setSource(source);
			handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			parserContext.getRegistry().registerBeanDefinition(HTTP_REQUEST_HANDLER_ADAPTER, handlerAdapterDef);
			parserContext.registerComponent(new BeanComponentDefinition(handlerAdapterDef, HTTP_REQUEST_HANDLER_ADAPTER));
		}
	}
	
	public static void registerSimpleControllerHandlerAdapter(ParserContext parserContext, Object source) {
		if (!parserContext.getRegistry().containsBeanDefinition(VIEW_CONTROLLER_HANDLER_ADAPTER)) {
			RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(SimpleControllerHandlerAdapter.class);
			handlerAdapterDef.setSource(source);
			handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			parserContext.getRegistry().registerBeanDefinition(VIEW_CONTROLLER_HANDLER_ADAPTER, handlerAdapterDef);
			parserContext.registerComponent(new BeanComponentDefinition(handlerAdapterDef, VIEW_CONTROLLER_HANDLER_ADAPTER));
		}
	}

}
