package org.springframework.web.servlet.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesView;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;
import org.w3c.dom.Element;


public class ViewResolversBeanDefinitionParser implements BeanDefinitionParser {
	
	private static final String INTERNAL_VIEW_RESOLVER_BEAN_NAME =
			"org.springframework.web.servlet.view.InternalResourceViewResolver";
	private static final String TILES3_VIEW_RESOLVER_BEAN_NAME =
			"org.springframework.web.servlet.view.tiles3.TilesViewResolver";
	private static final String TILES3_CONFIGURER_BEAN_NAME =
			"org.springframework.web.servlet.view.tiles3.TilesConfigurer";
	private static final String BEANNAME_VIEW_RESOLVER_BEAN_NAME =
			"org.springframework.web.servlet.view.BeanNameViewResolver";
	private static final String FREEMARKER_CONFIGURER_BEAN_NAME =
			"org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer";
	private static final String FREEMARKER_VIEW_RESOLVER_BEAN_NAME =
			"org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver";
	
	private ParserContext parserContext;
	private Object source;

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		
		this.parserContext=parserContext;
		
		
		
		 source= parserContext.extractSource(element);
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(),source);
		parserContext.pushContainingComponent(compDefinition);
		
		
		List<Element> viewResolverElements = DomUtils.getChildElementsByTagName(element, new String[] { "jsp", "tiles","bean-name","freemarker" });
		for (Element viewResolverElement : viewResolverElements) {		
			
			if ("jsp".equals(viewResolverElement.getLocalName())) {
				registerInternalResourceViewResolverBean(parserContext,viewResolverElement);				
			}		
			if ("tiles".equals(viewResolverElement.getLocalName())) {
				registerTilesViewResolverBean(parserContext,viewResolverElement);				
				registerTilesConfigurerBean(parserContext,viewResolverElement);
			}
			if("bean-name".equals(viewResolverElement.getLocalName())){
				registerBeanNameViewResolverBean(parserContext,viewResolverElement);
			}
			if("freemarker".equals(viewResolverElement.getLocalName())){
				registerFreemarkerViewResolverBean(parserContext,viewResolverElement);
				registerFreemarkerConfigurerBean(parserContext,viewResolverElement);
			}
		}
		
		MvcNamespaceUtils.registerDefaultComponents(parserContext, source);
		parserContext.popAndRegisterContainingComponent();
		return null;
		

	}
	
	private void registerBean(String beanName,Map<String,Object> propertyMap,Class beanClass ){
		RootBeanDefinition beanDef = new RootBeanDefinition(beanClass);
		beanDef.setSource(source);
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		
		for(String propertyName:propertyMap.keySet()){
			beanDef.getPropertyValues().add(propertyName, propertyMap.get(propertyName));
		}
		parserContext.getRegistry().registerBeanDefinition(beanName, beanDef);
		parserContext.registerComponent(new BeanComponentDefinition(beanDef, beanName));
	
		
	}
	
	private void registerFreemarkerConfigurerBean(ParserContext parserContext, Element viewResolverElement) {
		String templateLoaderPath=viewResolverElement.getAttribute("templateLoaderPath");
		Map<String, Object> propertyMap= new HashMap<String, Object>();
		propertyMap.put("templateLoaderPath", templateLoaderPath);
		
		registerBean(FREEMARKER_CONFIGURER_BEAN_NAME, propertyMap, FreeMarkerConfigurer.class);
		
	}

	private void registerFreemarkerViewResolverBean(ParserContext parserContext, 	Element viewResolverElement) {
			if (!parserContext.getRegistry().containsBeanDefinition(FREEMARKER_VIEW_RESOLVER_BEAN_NAME)) {
								
				Map<String, Object> propertyMap= new HashMap<String, Object>();
				propertyMap.put("prefix", viewResolverElement.getAttribute("prefix"));
				propertyMap.put("suffix", viewResolverElement.getAttribute("suffix"));
				propertyMap.put("order", 4);				
				registerBean(FREEMARKER_VIEW_RESOLVER_BEAN_NAME, propertyMap, FreeMarkerViewResolver.class);				
			}		
	}

	private void registerBeanNameViewResolverBean(ParserContext parserContext, Element viewResolverElement) {
		if (!parserContext.getRegistry().containsBeanDefinition(BEANNAME_VIEW_RESOLVER_BEAN_NAME)) {			
			Map<String, Object> propertyMap= new HashMap<String, Object>();			
			propertyMap.put("order", 3);
			registerBean(BEANNAME_VIEW_RESOLVER_BEAN_NAME, propertyMap, BeanNameViewResolver.class);		
		}		
	}

	private void registerTilesConfigurerBean(ParserContext parserContext,Element viewResolverElement) {
		if (!parserContext.getRegistry().containsBeanDefinition(TILES3_CONFIGURER_BEAN_NAME)) {			
			Map<String, Object> propertyMap= new HashMap<String, Object>();			
			propertyMap.put("definitions", viewResolverElement.getAttribute("definitions"));
			registerBean(TILES3_CONFIGURER_BEAN_NAME, propertyMap, TilesConfigurer.class);		
		}	
	}
	
	private void registerTilesViewResolverBean(ParserContext parserContext, Element viewResolverElement) {
		
		if (!parserContext.getRegistry().containsBeanDefinition(TILES3_VIEW_RESOLVER_BEAN_NAME)) {
			Map<String, Object> propertyMap= new HashMap<String, Object>();			
			propertyMap.put("viewClass", TilesView.class);
			propertyMap.put("order", 1);
			registerBean(TILES3_VIEW_RESOLVER_BEAN_NAME, propertyMap, TilesViewResolver.class);			
		}		
	}
	private void registerInternalResourceViewResolverBean(ParserContext parserContext, Element viewResolverElement) {
		if (!parserContext.getRegistry().containsBeanDefinition(INTERNAL_VIEW_RESOLVER_BEAN_NAME)) {
			Map<String, Object> propertyMap= new HashMap<String, Object>();
			propertyMap.put("prefix", viewResolverElement.getAttribute("prefix"));
			propertyMap.put("suffix", viewResolverElement.getAttribute("suffix"));
			propertyMap.put("order", 2);
			registerBean(INTERNAL_VIEW_RESOLVER_BEAN_NAME, propertyMap, InternalResourceViewResolver.class);			
		}
		

	}
	
	

}
