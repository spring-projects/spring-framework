package org.springframework.web.servlet.config;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resources.DefaultServletHttpRequestHandler;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} that parses a {@code default-servlet-handler} element to 
 * register a {@link DefaultServletHttpRequestHandler}.  Will also register a 
 * {@link SimpleUrlHandlerMapping} for mapping resource requests, and a 
 * {@link HttpRequestHandlerAdapter} if necessary. 
 * 
 * @author Jeremy Grelle
 * @since 3.0.4
 */
public class DefaultServletHandlerBeanDefinitionParser extends AbstractHttpRequestHandlerBeanDefinitionParser {

	@Override
	public void doParse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		
		String defaultServletName = element.getAttribute("default-servlet-name");		
		RootBeanDefinition defaultServletHandlerDef = new RootBeanDefinition(DefaultServletHttpRequestHandler.class);
		defaultServletHandlerDef.setSource(source);
		defaultServletHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		if (StringUtils.hasText(defaultServletName)) {
			defaultServletHandlerDef.getPropertyValues().add("defaultServletName", defaultServletName);
		}
		String defaultServletHandlerName = parserContext.getReaderContext().generateBeanName(defaultServletHandlerDef);
		parserContext.getRegistry().registerBeanDefinition(defaultServletHandlerName, defaultServletHandlerDef);
		parserContext.registerComponent(new BeanComponentDefinition(defaultServletHandlerDef, defaultServletHandlerName));
		
		Map<String, String> urlMap = new ManagedMap<String, String>();
		urlMap.put("/**", defaultServletHandlerName);
		
		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
		
		String handlerMappingBeanName = parserContext.getReaderContext().generateBeanName(handlerMappingDef);
		parserContext.getRegistry().registerBeanDefinition(handlerMappingBeanName, handlerMappingDef);
		parserContext.registerComponent(new BeanComponentDefinition(handlerMappingDef, handlerMappingBeanName));
	}

}
