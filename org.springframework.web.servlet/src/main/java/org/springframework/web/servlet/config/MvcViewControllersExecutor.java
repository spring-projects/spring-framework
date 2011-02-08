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
 */package org.springframework.web.servlet.config;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentRegistrar;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.config.AbstractSpecificationExecutor;
import org.springframework.context.config.ExecutorContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;

/**
 * Executes {@link MvcViewControllers} specification, creating and registering
 * bean definitions as appropriate based on the configuration within.
 *
 * @author Keith Donald
 * @author Christian Dupuis
 * @author Rossen Stoyanchev
 * @since 3.1
 */
final class MvcViewControllersExecutor extends AbstractSpecificationExecutor<MvcViewControllers> {

	private static final String HANDLER_ADAPTER_BEAN_NAME = "org.springframework.web.servlet.config.viewControllerHandlerAdapter";

	private static final String HANDLER_MAPPING_BEAN_NAME = "org.springframework.web.servlet.config.viewControllerHandlerMapping";

	@Override
	protected void doExecute(MvcViewControllers spec, ExecutorContext executorContext) {
		BeanDefinitionRegistry registry = executorContext.getRegistry();
		ComponentRegistrar registrar = executorContext.getRegistrar();
		Object source = spec.source();

		if (!registry.containsBeanDefinition(HANDLER_ADAPTER_BEAN_NAME)) {
			RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(SimpleControllerHandlerAdapter.class);
			handlerAdapterDef.setSource(source);
			handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(HANDLER_ADAPTER_BEAN_NAME, handlerAdapterDef);
			registrar.registerComponent(new BeanComponentDefinition(handlerAdapterDef, HANDLER_ADAPTER_BEAN_NAME));
		}

		BeanDefinition handlerMappingBeanDef = null;
		if (!registry.containsBeanDefinition(HANDLER_MAPPING_BEAN_NAME)) {
			RootBeanDefinition beanDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
			beanDef.setSource(source);
			beanDef.getPropertyValues().add("order", "1");
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME, beanDef);
			registrar.registerComponent(new BeanComponentDefinition(beanDef, HANDLER_MAPPING_BEAN_NAME));
			handlerMappingBeanDef = beanDef;
		} else {
			handlerMappingBeanDef = registry.getBeanDefinition(HANDLER_MAPPING_BEAN_NAME);
		}

		for (Map.Entry<String, String> entry : spec.mappings().entrySet()) {
			RootBeanDefinition viewControllerDef = new RootBeanDefinition(ParameterizableViewController.class);
			viewControllerDef.setSource(source);
			if (entry.getValue() != null) {
				viewControllerDef.getPropertyValues().add("viewName", entry.getValue());
			}
			if (!handlerMappingBeanDef.getPropertyValues().contains("urlMap")) {
				handlerMappingBeanDef.getPropertyValues().add("urlMap", new ManagedMap<String, BeanDefinition>());
			}
			@SuppressWarnings("unchecked")
			Map<String, BeanDefinition> urlMap = (Map<String, BeanDefinition>) handlerMappingBeanDef
					.getPropertyValues().getPropertyValue("urlMap").getValue();
			urlMap.put(entry.getKey(), viewControllerDef);
		}
	}

}
