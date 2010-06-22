package org.springframework.web.servlet.config;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resources.ResourceHttpRequestHandler;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a
 * {@code resources} element to register a {@link ResourceHttpRequestHandler}.
 * Will also register a {@link SimpleUrlHandlerMapping} for mapping resource requests, if necessary.
 * Will also register a {@link HttpRequestHandlerAdapter} if necessary. 
 *
 * @author Keith Donald
 * @since 3.0.4
 */
public class ResourcesBeanDefinitionParser implements BeanDefinitionParser {

	private static final String HANDLER_ADAPTER_BEAN_NAME = "org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter";

	private static final String HANDLER_MAPPING_BEAN_NAME = "org.springframework.web.servlet.config.resourcesHandlerMapping";

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		registerHandlerAdapterIfNecessary(parserContext, source);
		BeanDefinition handlerMappingDef = registerHandlerMappingIfNecessary(parserContext, source);

		String resourceDirectory = "/resources/";
		RootBeanDefinition resourceHandlerDef = new RootBeanDefinition(ResourceHttpRequestHandler.class);
		resourceHandlerDef.setSource(source);
		resourceHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		resourceHandlerDef.getConstructorArgumentValues().addIndexedArgumentValue(0, resourceDirectory);
		
		Map<String, BeanDefinition> urlMap = getUrlMap(handlerMappingDef);
		String resourceRequestPath = "/resources/**";
		urlMap.put(resourceRequestPath, resourceHandlerDef);

		return null;
	}

	private void registerHandlerAdapterIfNecessary(ParserContext parserContext, Object source) {
		if (!parserContext.getRegistry().containsBeanDefinition(HANDLER_ADAPTER_BEAN_NAME)) {
			RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(HttpRequestHandlerAdapter.class);
			handlerAdapterDef.setSource(source);
			handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			parserContext.getRegistry().registerBeanDefinition(HANDLER_ADAPTER_BEAN_NAME, handlerAdapterDef);
			parserContext.registerComponent(new BeanComponentDefinition(handlerAdapterDef, HANDLER_ADAPTER_BEAN_NAME));
		}
	}
	
	private BeanDefinition registerHandlerMappingIfNecessary(ParserContext parserContext, Object source) {
		if (!parserContext.getRegistry().containsBeanDefinition(HANDLER_MAPPING_BEAN_NAME)) {
			RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
			handlerMappingDef.setSource(source);
			handlerMappingDef.getPropertyValues().add("order", "2");
			handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			parserContext.getRegistry().registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME, handlerMappingDef);
			parserContext.registerComponent(new BeanComponentDefinition(handlerMappingDef, HANDLER_MAPPING_BEAN_NAME));
			return handlerMappingDef;
		}
		else {
			return parserContext.getRegistry().getBeanDefinition(HANDLER_MAPPING_BEAN_NAME);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, BeanDefinition> getUrlMap(BeanDefinition handlerMappingDef) {
		Map<String, BeanDefinition> urlMap;
		if (handlerMappingDef.getPropertyValues().contains("urlMap")) {
			urlMap = (Map<String, BeanDefinition>) handlerMappingDef.getPropertyValues().getPropertyValue("urlMap").getValue();
		}
		else {
			urlMap = new ManagedMap<String, BeanDefinition>();
			handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
		}
		return urlMap;
	}

}
