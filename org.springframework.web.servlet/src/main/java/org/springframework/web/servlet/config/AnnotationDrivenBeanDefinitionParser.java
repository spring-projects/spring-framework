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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.config.AbstractSpecificationBeanDefinitionParser;
import org.springframework.context.config.FeatureSpecification;
import org.springframework.util.ClassUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletWebArgumentResolverAdapter;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} that parses the {@code annotation-driven} element 
 * to configure a Spring MVC web application. 
 *
 * @author Rossen Stoyanchev
 * @author Chris Beams
 * @since 3.0
 * @see MvcAnnotationDriven
 * @see MvcAnnotationDrivenExecutor
 */
class AnnotationDrivenBeanDefinitionParser extends AbstractSpecificationBeanDefinitionParser {

	/**
	 * Parses the {@code <mvc:annotation-driven/>} tag.
	 */
	@Override
	protected FeatureSpecification doParse(Element element, ParserContext parserContext) {
		MvcAnnotationDriven spec = new MvcAnnotationDriven();
		if (element.hasAttribute("conversion-service")) {
			String conversionService = element.getAttribute("conversion-service");
			spec.conversionService(conversionService);
		}
		if (element.hasAttribute("validator")) {
			spec.validator(element.getAttribute("validator"));
		}
		if (element.hasAttribute("message-codes-resolver")) {
			spec.messageCodesResolver(element.getAttribute("message-codes-resolver"));
		}
		Element convertersElement = DomUtils.getChildElementByTagName(element, "message-converters");
		if (convertersElement != null) {
			if (convertersElement.hasAttribute("register-defaults")) {
				spec.shouldRegisterDefaultMessageConverters(Boolean.valueOf(convertersElement
						.getAttribute("register-defaults")));
			}
			spec.messageConverters(extractBeanSubElements(convertersElement, parserContext));
		}
		Element resolversElement = DomUtils.getChildElementByTagName(element, "argument-resolvers");
		if (resolversElement != null) {
			ManagedList<BeanDefinitionHolder> beanDefs = extractBeanSubElements(resolversElement, parserContext);
			spec.argumentResolvers(wrapWebArgumentResolverBeanDefs(beanDefs));
		}

		return spec;
	}

	private ManagedList<BeanDefinitionHolder> extractBeanSubElements(Element parentElement, ParserContext parserContext) {
		ManagedList<BeanDefinitionHolder> list = new ManagedList<BeanDefinitionHolder>();
		list.setSource(parserContext.extractSource(parentElement));
		for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, "bean")) {
			BeanDefinitionHolder beanDef = parserContext.getDelegate().parseBeanDefinitionElement(beanElement);
			beanDef = parserContext.getDelegate().decorateBeanDefinitionIfRequired(beanElement, beanDef);
			list.add(beanDef);
		}
		return list;
	}

	private ManagedList<BeanDefinitionHolder> wrapWebArgumentResolverBeanDefs(List<BeanDefinitionHolder> beanDefs) {
		ManagedList<BeanDefinitionHolder> result = new ManagedList<BeanDefinitionHolder>();
		
		for (BeanDefinitionHolder beanDef : beanDefs) {
			String className = beanDef.getBeanDefinition().getBeanClassName();
			Class<?> clazz = ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader());
			
			if (WebArgumentResolver.class.isAssignableFrom(clazz)) {
				RootBeanDefinition adapter = new RootBeanDefinition(ServletWebArgumentResolverAdapter.class);
				adapter.getConstructorArgumentValues().addIndexedArgumentValue(0, beanDef);
				result.add(new BeanDefinitionHolder(adapter, beanDef.getBeanName() + "Adapter"));
			}
			else {
				result.add(beanDef);
			}
		}
		
		return result;
	}
}
