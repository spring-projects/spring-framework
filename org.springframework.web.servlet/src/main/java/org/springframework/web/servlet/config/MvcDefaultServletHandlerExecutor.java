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
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

/**
 * Executes {@link MvcDefaultServletHandler} specifications, creating and
 * registering bean definitions as appropriate based on the configuration
 * within.
 * 
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 3.1
 */
final class MvcDefaultServletHandlerExecutor extends AbstractSpecificationExecutor<MvcDefaultServletHandler> {

	private static final String HANDLER_ADAPTER_BEAN_NAME = "org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter";

	@Override
	protected void doExecute(MvcDefaultServletHandler spec, ExecutorContext executorContext) {
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
		
		RootBeanDefinition defaultServletHandlerDef = new RootBeanDefinition(DefaultServletHttpRequestHandler.class);
		defaultServletHandlerDef.setSource(source);
		defaultServletHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		if (StringUtils.hasText(spec.defaultServletName())) {
			defaultServletHandlerDef.getPropertyValues().add("defaultServletName", spec.defaultServletName());
		}
		String defaultServletHandlerName = registrar.registerWithGeneratedName(defaultServletHandlerDef);
		registry.registerBeanDefinition(defaultServletHandlerName, defaultServletHandlerDef);
		registrar.registerComponent(new BeanComponentDefinition(defaultServletHandlerDef, defaultServletHandlerName));

		Map<String, String> urlMap = new ManagedMap<String, String>();
		urlMap.put("/**", defaultServletHandlerName);
		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
		String handlerMappingBeanName = registrar.registerWithGeneratedName(handlerMappingDef);
		registry.registerBeanDefinition(handlerMappingBeanName, handlerMappingDef);
		registrar.registerComponent(new BeanComponentDefinition(handlerMappingDef, handlerMappingBeanName));
	}

}
