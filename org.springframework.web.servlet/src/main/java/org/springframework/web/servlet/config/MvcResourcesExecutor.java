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
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Executes {@link MvcResources} specifications, creating and registering
 * bean definitions as appropriate based on the configuration within.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 3.1
 */
final class MvcResourcesExecutor extends AbstractSpecificationExecutor<MvcResources> {

	private static final String HANDLER_ADAPTER_BEAN_NAME = "org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter";

	@Override
	protected void doExecute(MvcResources spec, ExecutorContext executorContext) {
		BeanDefinitionRegistry registry = executorContext.getRegistry();
		ComponentRegistrar registrar = executorContext.getRegistrar();
		Object source = spec.source();

		if (!registry.containsBeanDefinition(HANDLER_ADAPTER_BEAN_NAME)) {
			RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(HttpRequestHandlerAdapter.class);
			handlerAdapterDef.setSource(source);
			handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(HANDLER_ADAPTER_BEAN_NAME, handlerAdapterDef);
			registrar.registerComponent(new BeanComponentDefinition(handlerAdapterDef, HANDLER_ADAPTER_BEAN_NAME));
		}

		RootBeanDefinition resourceHandlerDef = new RootBeanDefinition(ResourceHttpRequestHandler.class);
		resourceHandlerDef.setSource(source);
		resourceHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		resourceHandlerDef.getPropertyValues().add("locations", spec.locations());
		if (spec.cachePeriod() != null) {
			resourceHandlerDef.getPropertyValues().add("cacheSeconds", spec.cachePeriod());
		}
		String resourceHandlerBeanName = registrar.registerWithGeneratedName(resourceHandlerDef);
		registry.registerBeanDefinition(resourceHandlerBeanName, resourceHandlerDef);
		registrar.registerComponent(new BeanComponentDefinition(resourceHandlerDef, resourceHandlerBeanName));

		Map<String, String> urlMap = new ManagedMap<String, String>();
		urlMap.put(spec.mapping(), resourceHandlerBeanName);
		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
		if (spec.order() != null) {
			handlerMappingDef.getPropertyValues().add("order", spec.order());
		}
		String beanName = registrar.registerWithGeneratedName(handlerMappingDef);
		registry.registerBeanDefinition(beanName, handlerMappingDef);
		registrar.registerComponent(new BeanComponentDefinition(handlerMappingDef, beanName));
	}

}
