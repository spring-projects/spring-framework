/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.OrderComparator;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;


/**
 *
 * @author Jeremy Grelle
 * @since 4.0
 */
public class ResourceUrlMapper implements BeanPostProcessor, ApplicationListener<ContextRefreshedEvent>{

	private final Map<String, ResourceHttpRequestHandler> handlers = new LinkedHashMap<String, ResourceHttpRequestHandler>();

	private final List<SimpleUrlHandlerMapping> mappings = new ArrayList<SimpleUrlHandlerMapping>();

	private final PathMatcher matcher = new AntPathMatcher();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (ClassUtils.isAssignableValue(SimpleUrlHandlerMapping.class, bean)) {
			SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) bean;
			for(Entry<String, ?> mappingEntry : mapping.getUrlMap().entrySet()) {
				Object val = mappingEntry.getValue();
				if (val instanceof ResourceHttpRequestHandler) {
					this.mappings.add(mapping);
				}
			}
		}
		return bean;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		OrderComparator.sort(this.mappings);
		for (SimpleUrlHandlerMapping mapping : mappings) {
			for(Entry<String, ?> mappingEntry : mapping.getUrlMap().entrySet()) {
				Object val = mappingEntry.getValue();
				this.handlers.put(mappingEntry.getKey(), (ResourceHttpRequestHandler) val);
			}
		}
	}

	public String getUrlForResource(String resourcePath) {
		for (Entry<String, ResourceHttpRequestHandler> mapping : this.handlers.entrySet()) {
			if (matcher.match(mapping.getKey(), resourcePath)) {
				ResourceHttpRequestHandler handler = mapping.getValue();
				String nestedPath = matcher.extractPathWithinPattern(mapping.getKey(), resourcePath);
				String prefix = resourcePath.replace(nestedPath, "");
				String url = new DefaultResourceResolverChain(handler.getResourceResolvers(), handler.
						getResourceTransformers()).resolveUrl(nestedPath, handler.getLocations());
				if (url != null) {
					return prefix + url;
				}
			}
		}
		return null;
	}

	public boolean isResourceUrl(String relativeUrl) {
		for (String mapping : this.handlers.keySet()) {
			if (matcher.match(mapping, relativeUrl)) {
				return true;
			}
		}
		return false;
	}
}
