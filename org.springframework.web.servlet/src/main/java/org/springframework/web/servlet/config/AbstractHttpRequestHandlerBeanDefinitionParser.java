package org.springframework.web.servlet.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.w3c.dom.Element;

/**
 * Abstract base class for {@link BeanDefinitonParser}s that register an {@link HttpRequestHandler}.
 * 
 * @author Jeremy Grelle
 * @since 3.0.4
 */
public abstract class AbstractHttpRequestHandlerBeanDefinitionParser implements BeanDefinitionParser{

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
