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

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a {@code interceptors} element to register
 * a set of {@link MappedInterceptor} definitions.
 * 
 * @author Keith Donald
 * @since 3.0
 */
class InterceptorsBeanDefinitionParser implements BeanDefinitionParser {

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), parserContext.extractSource(element));
		parserContext.pushContainingComponent(compDefinition);
		
		List<Element> interceptors = DomUtils.getChildElementsByTagName(element, new String[] { "bean", "interceptor" });
		for (Element interceptor : interceptors) {
			RootBeanDefinition mappedInterceptorDef = new RootBeanDefinition(MappedInterceptor.class);
			mappedInterceptorDef.setSource(parserContext.extractSource(interceptor));
			mappedInterceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String[] pathPatterns;
			BeanDefinitionHolder interceptorDef;
			if ("interceptor".equals(interceptor.getLocalName())) {
				List<Element> paths = DomUtils.getChildElementsByTagName(interceptor, "mapping");
				pathPatterns = new String[paths.size()];
				for (int i = 0; i < paths.size(); i++) {
					pathPatterns[i] = paths.get(i).getAttribute("path");
				}
				Element interceptorBean = DomUtils.getChildElementByTagName(interceptor, "bean");
				interceptorDef = parserContext.getDelegate().parseBeanDefinitionElement(interceptorBean);
				interceptorDef = parserContext.getDelegate().decorateBeanDefinitionIfRequired(interceptorBean, interceptorDef);
			} else {
				pathPatterns = null;
				interceptorDef = parserContext.getDelegate().parseBeanDefinitionElement(interceptor);
				interceptorDef = parserContext.getDelegate().decorateBeanDefinitionIfRequired(interceptor, interceptorDef);				
			}
			mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, pathPatterns);
			mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(1, interceptorDef);
			String mappedInterceptorName = parserContext.getReaderContext().registerWithGeneratedName(mappedInterceptorDef);
			parserContext.registerComponent(new BeanComponentDefinition(mappedInterceptorDef, mappedInterceptorName));
		}
		
		parserContext.popAndRegisterContainingComponent();
		return null;
	}
	
}
