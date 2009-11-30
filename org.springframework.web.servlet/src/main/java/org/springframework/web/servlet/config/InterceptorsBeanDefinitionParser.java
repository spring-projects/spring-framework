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
		List<Element> interceptors = DomUtils.getChildElementsByTagName(element, "interceptor");
		for (Element interceptor : interceptors) {
			RootBeanDefinition mappedInterceptorDef = new RootBeanDefinition(MappedInterceptor.class);
			mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, interceptor.getAttribute("path"));
			RootBeanDefinition interceptorDef = new RootBeanDefinition(interceptor.getAttribute("class"));
			BeanDefinitionHolder holder = new BeanDefinitionHolder(interceptorDef, parserContext.getReaderContext().generateBeanName(interceptorDef));
			holder = parserContext.getDelegate().decorateBeanDefinitionIfRequired(interceptor, holder);
			parserContext.getDelegate().parseConstructorArgElements(interceptor, interceptorDef);
			parserContext.getDelegate().parsePropertyElements(interceptor, interceptorDef);
			mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(1, holder);
			parserContext.getReaderContext().registerWithGeneratedName(mappedInterceptorDef);
		}
		return null;
	}
	
}
