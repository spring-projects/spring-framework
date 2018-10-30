/*
 * Copyright 2002-2018 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Ordered;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;

/**
 * Parse the {@code view-resolvers} MVC namespace element and register
 * {@link org.springframework.web.servlet.ViewResolver} bean definitions.
 *
 * <p>All registered resolvers are wrapped in a single (composite) ViewResolver
 * with its order property set to 0 so that other external resolvers may be ordered
 * before or after it.
 *
 * <p>When content negotiation is enabled the order property is set to highest priority
 * instead with the ContentNegotiatingViewResolver encapsulating all other registered
 * view resolver instances. That way the resolvers registered through the MVC namespace
 * form self-encapsulated resolver chain.
 *
 * @author Sivaprasad Valluru
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see TilesConfigurerBeanDefinitionParser
 * @see FreeMarkerConfigurerBeanDefinitionParser
 * @see GroovyMarkupConfigurerBeanDefinitionParser
 * @see ScriptTemplateConfigurerBeanDefinitionParser
 */
public class ViewResolversBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * The bean name used for the {@code ViewResolverComposite}.
	 */
	public static final String VIEW_RESOLVER_BEAN_NAME = "mvcViewResolver";


	public BeanDefinition parse(Element element, ParserContext context) {
		Object source = context.extractSource(element);
		context.pushContainingComponent(new CompositeComponentDefinition(element.getTagName(), source));

		ManagedList<Object> resolvers = new ManagedList<>(4);
		resolvers.setSource(context.extractSource(element));
		String[] names = new String[] {
				"jsp", "tiles", "bean-name", "freemarker", "groovy", "script-template", "bean", "ref"};

		for (Element resolverElement : DomUtils.getChildElementsByTagName(element, names)) {
			String name = resolverElement.getLocalName();
			if ("bean".equals(name) || "ref".equals(name)) {
				resolvers.add(context.getDelegate().parsePropertySubElement(resolverElement, null));
				continue;
			}
			RootBeanDefinition resolverBeanDef;
			if ("jsp".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(InternalResourceViewResolver.class);
				resolverBeanDef.getPropertyValues().add("prefix", "/WEB-INF/");
				resolverBeanDef.getPropertyValues().add("suffix", ".jsp");
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("tiles".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(TilesViewResolver.class);
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("freemarker".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(FreeMarkerViewResolver.class);
				resolverBeanDef.getPropertyValues().add("suffix", ".ftl");
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("groovy".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(GroovyMarkupViewResolver.class);
				resolverBeanDef.getPropertyValues().add("suffix", ".tpl");
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("script-template".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(ScriptTemplateViewResolver.class);
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("bean-name".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(BeanNameViewResolver.class);
			}
			else {
				// Should never happen
				throw new IllegalStateException("Unexpected element name: " + name);
			}
			resolverBeanDef.setSource(source);
			resolverBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			resolvers.add(resolverBeanDef);
		}

		String beanName = VIEW_RESOLVER_BEAN_NAME;
		RootBeanDefinition compositeResolverBeanDef = new RootBeanDefinition(ViewResolverComposite.class);
		compositeResolverBeanDef.setSource(source);
		compositeResolverBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		names = new String[] {"content-negotiation"};
		List<Element> contentNegotiationElements = DomUtils.getChildElementsByTagName(element, names);
		if (contentNegotiationElements.isEmpty()) {
			compositeResolverBeanDef.getPropertyValues().add("viewResolvers", resolvers);
		}
		else if (contentNegotiationElements.size() == 1) {
			BeanDefinition beanDef = createContentNegotiatingViewResolver(contentNegotiationElements.get(0), context);
			beanDef.getPropertyValues().add("viewResolvers", resolvers);
			ManagedList<Object> list = new ManagedList<>(1);
			list.add(beanDef);
			compositeResolverBeanDef.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
			compositeResolverBeanDef.getPropertyValues().add("viewResolvers", list);
		}
		else {
			throw new IllegalArgumentException("Only one <content-negotiation> element is allowed.");
		}

		if (element.hasAttribute("order")) {
			compositeResolverBeanDef.getPropertyValues().add("order", element.getAttribute("order"));
		}

		context.getReaderContext().getRegistry().registerBeanDefinition(beanName, compositeResolverBeanDef);
		context.registerComponent(new BeanComponentDefinition(compositeResolverBeanDef, beanName));
		context.popAndRegisterContainingComponent();
		return null;
	}

	private void addUrlBasedViewResolverProperties(Element element, RootBeanDefinition beanDefinition) {
		if (element.hasAttribute("prefix")) {
			beanDefinition.getPropertyValues().add("prefix", element.getAttribute("prefix"));
		}
		if (element.hasAttribute("suffix")) {
			beanDefinition.getPropertyValues().add("suffix", element.getAttribute("suffix"));
		}
		if (element.hasAttribute("cache-views")) {
			beanDefinition.getPropertyValues().add("cache", element.getAttribute("cache-views"));
		}
		if (element.hasAttribute("view-class")) {
			beanDefinition.getPropertyValues().add("viewClass", element.getAttribute("view-class"));
		}
		if (element.hasAttribute("view-names")) {
			beanDefinition.getPropertyValues().add("viewNames", element.getAttribute("view-names"));
		}
	}

	private BeanDefinition createContentNegotiatingViewResolver(Element resolverElement, ParserContext context) {
		RootBeanDefinition beanDef = new RootBeanDefinition(ContentNegotiatingViewResolver.class);
		beanDef.setSource(context.extractSource(resolverElement));
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		MutablePropertyValues values = beanDef.getPropertyValues();

		List<Element> elements = DomUtils.getChildElementsByTagName(resolverElement, "default-views");
		if (!elements.isEmpty()) {
			ManagedList<Object> list = new ManagedList<>();
			for (Element element : DomUtils.getChildElementsByTagName(elements.get(0), "bean", "ref")) {
				list.add(context.getDelegate().parsePropertySubElement(element, null));
			}
			values.add("defaultViews", list);
		}
		if (resolverElement.hasAttribute("use-not-acceptable")) {
			values.add("useNotAcceptableStatusCode", resolverElement.getAttribute("use-not-acceptable"));
		}
		Object manager = MvcNamespaceUtils.getContentNegotiationManager(context);
		if (manager != null) {
			values.add("contentNegotiationManager", manager);
		}
		return beanDef;
	}

}
