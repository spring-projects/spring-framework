package org.springframework.web.servlet.config;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resources.ResourceHttpRequestHandler;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a
 * {@code resources} element to register a {@link ResourceHttpRequestHandler}.
 * Will also register a {@link SimpleUrlHandlerMapping} for mapping resource requests, 
 * and a {@link HttpRequestHandlerAdapter} if necessary. 
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @since 3.0.4
 */
public class ResourcesBeanDefinitionParser extends AbstractHttpRequestHandlerBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	public void doParse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		registerResourceMappings(parserContext, element, source);
	}
	
	private void registerResourceMappings(ParserContext parserContext, Element element, Object source) {
		String resourceHandlerName = registerResourceHandler(parserContext, element, source);
		if (!StringUtils.hasText(resourceHandlerName)) {
			return;
		}
		
		Map<String, String> urlMap = new ManagedMap<String, String>();
		String resourceRequestPath = element.getAttribute("mapping");
		if (!StringUtils.hasText(resourceRequestPath)) {
			parserContext.getReaderContext().error("The 'mapping' attribute is required.", parserContext.extractSource(element));
	        return;
		}
		urlMap.put(resourceRequestPath, resourceHandlerName);
		
		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
		
		String mappingOrder = element.getAttribute("mapping-order");
		handlerMappingDef.getPropertyValues().add("order", StringUtils.hasText(mappingOrder) ? mappingOrder : Ordered.LOWEST_PRECEDENCE - 1);
		
		String beanName = parserContext.getReaderContext().generateBeanName(handlerMappingDef);
		parserContext.getRegistry().registerBeanDefinition(beanName, handlerMappingDef);
		parserContext.registerComponent(new BeanComponentDefinition(handlerMappingDef, beanName));	
	}
	
	private String registerResourceHandler(ParserContext parserContext, Element element, Object source) {
		String locationAttr = element.getAttribute("location");
		if (!StringUtils.hasText(locationAttr)) {
			parserContext.getReaderContext().error("The 'location' attribute is required.", parserContext.extractSource(element));
	        return "";
		}		
		String[] locationPatterns = locationAttr.split(",\\s*");
		List<String> locations = new ManagedList<String>();
		for (String location : locationPatterns) {
			locations.add(location);
		}
		RootBeanDefinition resourceHandlerDef = new RootBeanDefinition(ResourceHttpRequestHandler.class);
		resourceHandlerDef.setSource(source);
		resourceHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		resourceHandlerDef.getConstructorArgumentValues().addIndexedArgumentValue(0, locations);
		String beanName = parserContext.getReaderContext().generateBeanName(resourceHandlerDef);
		parserContext.getRegistry().registerBeanDefinition(beanName, resourceHandlerDef);
		parserContext.registerComponent(new BeanComponentDefinition(resourceHandlerDef, beanName));	
		return beanName;
	}
}
