/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.cache.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * {@code NamespaceHandler} allowing for the configuration of declarative
 * cache management using either XML or using annotations.
 *
 * <p>This namespace handler is the central piece of functionality in the
 * Spring cache management facilities.
 *
 * @author Costin Leau
 * @since 3.1
 */
public class CacheNamespaceHandler extends NamespaceHandlerSupport {

	static final String CACHE_MANAGER_ATTRIBUTE = "cache-manager";
	static final String DEFAULT_CACHE_MANAGER_BEAN_NAME = "cacheManager";

	static String extractCacheManager(Element element) {
		return (element.hasAttribute(CacheNamespaceHandler.CACHE_MANAGER_ATTRIBUTE) ? element
				.getAttribute(CacheNamespaceHandler.CACHE_MANAGER_ATTRIBUTE)
				: CacheNamespaceHandler.DEFAULT_CACHE_MANAGER_BEAN_NAME);
	}

	static BeanDefinition parseKeyGenerator(Element element, BeanDefinition def) {
		String name = element.getAttribute("key-generator");
		if (StringUtils.hasText(name)) {
			def.getPropertyValues().add("keyGenerator", new RuntimeBeanReference(name.trim()));
		}
		return def;
	}

	@Override
	public void init() {
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenCacheBeanDefinitionParser());
		registerBeanDefinitionParser("advice", new CacheAdviceParser());
	}
}
