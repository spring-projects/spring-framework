/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that
 * parses the following MVC namespace elements:
 * <ul>
 * <li>{@code <view-controller>}
 * <li>{@code <redirect-view-controller>}
 * <li>{@code <status-controller>}
 * </ul>
 *
 * <p>All elements result in the registration of a
 * {@link org.springframework.web.servlet.mvc.ParameterizableViewController
 * ParameterizableViewController} with all controllers mapped using in a single
 * {@link org.springframework.web.servlet.handler.SimpleUrlHandlerMapping
 * SimpleUrlHandlerMapping}.
 *
 * @author Keith Donald
 * @author Christian Dupuis
 * @author Rossen Stoyanchev
 * @since 3.0
 */
class ViewControllerBeanDefinitionParser implements BeanDefinitionParser {

	private static final String HANDLER_MAPPING_BEAN_NAME =
			"org.springframework.web.servlet.config.viewControllerHandlerMapping";


	@Override
	@SuppressWarnings("unchecked")
	public @Nullable BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		// Register SimpleUrlHandlerMapping for view controllers
		BeanDefinition hm = registerHandlerMapping(parserContext, source);

		// Ensure BeanNameUrlHandlerMapping (SPR-8289) and default HandlerAdapters are not "turned off"
		MvcNamespaceUtils.registerDefaultComponents(parserContext, source);

		// Create view controller bean definition
		RootBeanDefinition controller = new RootBeanDefinition(ParameterizableViewController.class);
		controller.setSource(source);

		HttpStatusCode statusCode = null;
		if (element.hasAttribute("status-code")) {
			int statusValue = Integer.parseInt(element.getAttribute("status-code"));
			statusCode = HttpStatusCode.valueOf(statusValue);
		}

		String name = element.getLocalName();
		switch (name) {
			case "view-controller" -> {
				if (element.hasAttribute("view-name")) {
					controller.getPropertyValues().add("viewName", element.getAttribute("view-name"));
				}
				if (statusCode != null) {
					controller.getPropertyValues().add("statusCode", statusCode);
				}
			}
			case "redirect-view-controller" ->
				controller.getPropertyValues().add("view", getRedirectView(element, statusCode, source));
			case "status-controller" -> {
				controller.getPropertyValues().add("statusCode", statusCode);
				controller.getPropertyValues().add("statusOnly", true);
			}
			default ->
				// Should never happen...
				throw new IllegalStateException("Unexpected tag name: " + name);
		}

		Map<String, BeanDefinition> urlMap = (Map<String, BeanDefinition>) hm.getPropertyValues().get("urlMap");
		if (urlMap == null) {
			urlMap = new ManagedMap<>();
			hm.getPropertyValues().add("urlMap", urlMap);
		}
		urlMap.put(element.getAttribute("path"), controller);

		return null;
	}

	private BeanDefinition registerHandlerMapping(ParserContext context, @Nullable Object source) {
		if (context.getRegistry().containsBeanDefinition(HANDLER_MAPPING_BEAN_NAME)) {
			return context.getRegistry().getBeanDefinition(HANDLER_MAPPING_BEAN_NAME);
		}
		RootBeanDefinition beanDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		context.getRegistry().registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME, beanDef);
		context.registerComponent(new BeanComponentDefinition(beanDef, HANDLER_MAPPING_BEAN_NAME));

		beanDef.setSource(source);
		beanDef.getPropertyValues().add("order", "1");
		MvcNamespaceUtils.configurePathMatching(beanDef, context, source);
		beanDef.getPropertyValues().add("urlPathHelper", MvcNamespaceUtils.registerUrlPathHelper(null, context, source));
		RuntimeBeanReference corsConfigurationsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
		beanDef.getPropertyValues().add("corsConfigurations", corsConfigurationsRef);

		return beanDef;
	}

	private RootBeanDefinition getRedirectView(Element element, @Nullable HttpStatusCode status, @Nullable Object source) {
		RootBeanDefinition redirectView = new RootBeanDefinition(RedirectView.class);
		redirectView.setSource(source);
		redirectView.getConstructorArgumentValues().addIndexedArgumentValue(0, element.getAttribute("redirect-url"));

		if (status != null) {
			redirectView.getPropertyValues().add("statusCode", status);
		}

		if (element.hasAttribute("context-relative")) {
			redirectView.getPropertyValues().add("contextRelative", element.getAttribute("context-relative"));
		}
		else {
			redirectView.getPropertyValues().add("contextRelative", true);
		}

		if (element.hasAttribute("keep-query-params")) {
			redirectView.getPropertyValues().add("propagateQueryParams", element.getAttribute("keep-query-params"));
		}

		return redirectView;
	}

}
