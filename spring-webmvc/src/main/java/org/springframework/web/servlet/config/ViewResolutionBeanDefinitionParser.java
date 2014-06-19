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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.ViewResolver;

import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a
 * {@code view-resolution} element to register a set of {@link ViewResolver} definitions.
 *
 * @author Sivaprasad Valluru
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class ViewResolutionBeanDefinitionParser implements BeanDefinitionParser {

	private Object source;

	public BeanDefinition parse(Element element, ParserContext parserContext) {

		source= parserContext.extractSource(element);
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(),source);
		parserContext.pushContainingComponent(compDefinition);

		List<Element> viewResolverElements =
				DomUtils.getChildElementsByTagName(element, new String[] { "jsp", "tiles", "bean-name", "freemarker", "velocity", "bean", "ref" });
		ManagedList<Object> viewResolvers = new ManagedList<Object>();
		viewResolvers.setSource(parserContext.extractSource(element));
		int order = 0;

		for (Element viewResolverElement : viewResolverElements) {
			if ("jsp".equals(viewResolverElement.getLocalName())) {
				viewResolvers.add(registerInternalResourceViewResolverBean(viewResolverElement, parserContext, order));
			}
			if("bean-name".equals(viewResolverElement.getLocalName())){
				viewResolvers.add(registerBeanNameViewResolverBean(viewResolverElement, parserContext, order));
			}
			if ("tiles".equals(viewResolverElement.getLocalName())) {
				viewResolvers.add(registerTilesViewResolverBean(viewResolverElement, parserContext, order));
				registerTilesConfigurerBean(viewResolverElement, parserContext);
			}
			if("freemarker".equals(viewResolverElement.getLocalName())){
				viewResolvers.add(registerFreemarkerViewResolverBean(viewResolverElement, parserContext, order));
				registerFreemarkerConfigurerBean(viewResolverElement, parserContext);
			}
			if("velocity".equals(viewResolverElement.getLocalName())){
				viewResolvers.add(registerVelocityViewResolverBean(viewResolverElement, parserContext, order));
				registerVelocityConfigurerBean(viewResolverElement, parserContext);
			}
			if("bean".equals(viewResolverElement.getLocalName()) || "ref".equals(viewResolverElement.getLocalName())){
				viewResolvers.add(parserContext.getDelegate().parsePropertySubElement(viewResolverElement, null));
			}

			order++;
		}
		viewResolverElements = DomUtils.getChildElementsByTagName(element, new String[] { "content-negotiating" });
		if(!viewResolverElements.isEmpty()) {
			registerContentNegotiatingViewResolverBean(viewResolverElements.get(0), parserContext, viewResolvers);
		}

		parserContext.popAndRegisterContainingComponent();
		return null;
	}


	private BeanDefinition registerBean(Map<String,Object> propertyMap,Class<?> beanClass, ParserContext parserContext ){
		RootBeanDefinition beanDef = new RootBeanDefinition(beanClass);
		beanDef.setSource(source);
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		
		for(String propertyName:propertyMap.keySet()){
			beanDef.getPropertyValues().add(propertyName, propertyMap.get(propertyName));
		}
		String beanName = parserContext.getReaderContext().generateBeanName(beanDef);
		parserContext.getRegistry().registerBeanDefinition(beanName, beanDef);
		parserContext.registerComponent(new BeanComponentDefinition(beanDef, beanName));
		return beanDef;
	}

	private BeanDefinition registerContentNegotiatingViewResolverBean(Element viewResolverElement, ParserContext parserContext, ManagedList<Object> viewResolvers) {
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		ManagedList<Object> defaultViewBeanDefinitions = new ManagedList<Object>();
		List<Element> defaultViewElements =
				DomUtils.getChildElementsByTagName(viewResolverElement, new String[] { "default-views" });
		if(!defaultViewElements.isEmpty()) {
			for(Element beanElem : DomUtils.getChildElementsByTagName(defaultViewElements.get(0), "bean", "ref")) {
				defaultViewBeanDefinitions.add(parserContext.getDelegate().parsePropertySubElement(beanElem, null));
			}
		}
		if(viewResolverElement.hasAttribute("use-not-acceptable")) {
			propertyMap.put("useNotAcceptableStatusCode", viewResolverElement.getAttribute("use-not-acceptable"));
		}
		if(viewResolverElement.hasAttribute("manager")) {
			propertyMap.put("contentNegotiationManager", new RuntimeBeanReference(viewResolverElement.getAttribute("manager")));
		} else {
			propertyMap.put("contentNegotiationManager", new RuntimeBeanReference("mvcContentNegotiationManager"));
		}
		if(viewResolvers != null && !viewResolvers.isEmpty()) {
			propertyMap.put("viewResolvers", viewResolvers);
		}
		if(defaultViewBeanDefinitions != null && !defaultViewBeanDefinitions.isEmpty()) {
			propertyMap.put("defaultViews", defaultViewBeanDefinitions);
		}
		return registerBean(propertyMap, ContentNegotiatingViewResolver.class, parserContext);
	}
	
	private BeanDefinition registerFreemarkerConfigurerBean(Element viewResolverElement, ParserContext parserContext) {
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		if(viewResolverElement.hasAttribute("template-loader-paths")) {
			String[] paths = StringUtils.commaDelimitedListToStringArray(viewResolverElement.getAttribute("template-loader-paths"));
			propertyMap.put("templateLoaderPaths", paths);
		} else {
			propertyMap.put("templateLoaderPaths", new String[]{"/WEB-INF/"});
		}
		return registerBean(propertyMap, FreeMarkerConfigurer.class, parserContext);
	}

	private BeanDefinition registerFreemarkerViewResolverBean(Element viewResolverElement, ParserContext parserContext, int order) {
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		if(viewResolverElement.hasAttribute("prefix")) {
			propertyMap.put("prefix", viewResolverElement.getAttribute("prefix"));
		}
		if(viewResolverElement.hasAttribute("suffix")) {
			propertyMap.put("suffix", viewResolverElement.getAttribute("suffix"));
		}
		else {
			propertyMap.put("suffix", ".ftl");
		}
		if(viewResolverElement.hasAttribute("cache")) {
			propertyMap.put("cache", viewResolverElement.getAttribute("cache"));
		}
		propertyMap.put("order", order);
		return registerBean(propertyMap, FreeMarkerViewResolver.class, parserContext);
	}

	private BeanDefinition registerVelocityConfigurerBean(Element viewResolverElement, ParserContext parserContext) {
		String resourceLoaderPath = viewResolverElement.getAttribute("resource-loader-path");
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		if(viewResolverElement.hasAttribute("resource-loader-path")) {
			propertyMap.put("resourceLoaderPath", resourceLoaderPath);
		} else {
			propertyMap.put("resourceLoaderPath", "/WEB-INF/");
		}
		return registerBean(propertyMap, VelocityConfigurer.class, parserContext);
	}

	private BeanDefinition registerVelocityViewResolverBean(Element viewResolverElement, ParserContext parserContext, int order) {
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		if(viewResolverElement.hasAttribute("prefix")) {
			propertyMap.put("prefix", viewResolverElement.getAttribute("prefix"));
		}
		if(viewResolverElement.hasAttribute("suffix")) {
			propertyMap.put("suffix", viewResolverElement.getAttribute("suffix"));
		}
		else {
			propertyMap.put("suffix", ".vm");
		}
		if(viewResolverElement.hasAttribute("cache")) {
			propertyMap.put("cache", viewResolverElement.getAttribute("cache"));
		}
		propertyMap.put("order", order);
		return registerBean(propertyMap, VelocityViewResolver.class, parserContext);
	}

	private BeanDefinition registerBeanNameViewResolverBean(Element viewResolverElement, ParserContext parserContext, int order) {
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		propertyMap.put("order", order);
		return registerBean(propertyMap, BeanNameViewResolver.class, parserContext);
	}

	private BeanDefinition registerTilesConfigurerBean(Element viewResolverElement, ParserContext parserContext) {
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		if(viewResolverElement.hasAttribute("definitions")) {
			String[] definitions = StringUtils.commaDelimitedListToStringArray(viewResolverElement.getAttribute("definitions"));
			propertyMap.put("definitions", definitions);
		}
		if(viewResolverElement.hasAttribute("check-refresh")) {
			propertyMap.put("checkRefresh", viewResolverElement.getAttribute("check-refresh"));
		}
		return registerBean(propertyMap, TilesConfigurer.class, parserContext);
	}
	
	private BeanDefinition registerTilesViewResolverBean(Element viewResolverElement, ParserContext parserContext, int order) {
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		if(viewResolverElement.hasAttribute("prefix")) {
			propertyMap.put("prefix", viewResolverElement.getAttribute("prefix"));
		}
		if(viewResolverElement.hasAttribute("suffix")) {
			propertyMap.put("suffix", viewResolverElement.getAttribute("suffix"));
		}
		propertyMap.put("order", order);
		return registerBean(propertyMap, TilesViewResolver.class, parserContext);
	}

	private BeanDefinition registerInternalResourceViewResolverBean(Element viewResolverElement, ParserContext parserContext, int order) {
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		if(viewResolverElement.hasAttribute("prefix")) {
			propertyMap.put("prefix", viewResolverElement.getAttribute("prefix"));
		}
		else {
			propertyMap.put("prefix", "/WEB-INF/");
		}
		if(viewResolverElement.hasAttribute("suffix")) {
			propertyMap.put("suffix", viewResolverElement.getAttribute("suffix"));
		}
		else {
			propertyMap.put("suffix", ".jsp");
		}
		propertyMap.put("order", order);
		return registerBean(propertyMap, InternalResourceViewResolver.class, parserContext);
	}

}
