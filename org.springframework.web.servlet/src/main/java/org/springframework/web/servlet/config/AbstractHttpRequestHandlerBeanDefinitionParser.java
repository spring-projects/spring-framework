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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;

/**
 * Abstract base class for {@link BeanDefinitonParser}s that register an HttpRequestHandler.
 * 
 * @author Jeremy Grelle
 * @since 3.0.4
 */
abstract class AbstractHttpRequestHandlerBeanDefinitionParser implements BeanDefinitionParser{

	private static final String HANDLER_ADAPTER_BEAN_NAME = "org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter";
	
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		registerHandlerAdapterIfNecessary(parserContext, source);
		doParse(element, parserContext);
		return null;
	}
	
	public abstract void doParse(Element element, ParserContext parserContext);
	
	private void registerHandlerAdapterIfNecessary(ParserContext parserContext, Object source) {
		if (!parserContext.getRegistry().containsBeanDefinition(HANDLER_ADAPTER_BEAN_NAME)) {
			RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(HttpRequestHandlerAdapter.class);
			handlerAdapterDef.setSource(source);
			handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			parserContext.getRegistry().registerBeanDefinition(HANDLER_ADAPTER_BEAN_NAME, handlerAdapterDef);
			parserContext.registerComponent(new BeanComponentDefinition(handlerAdapterDef, HANDLER_ADAPTER_BEAN_NAME));
		}
	}

}
