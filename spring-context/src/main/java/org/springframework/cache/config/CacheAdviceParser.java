/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.cache.interceptor.NameMatchCacheOperationSource;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} for the {@code <tx:advice/>} tag.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class CacheAdviceParser extends AbstractSingleBeanDefinitionParser {

	private static final String CACHEABLE_ELEMENT = "cacheable";

	private static final String CACHE_EVICT_ELEMENT = "cache-evict";

	private static final String CACHE_PUT_ELEMENT = "cache-put";

	private static final String METHOD_ATTRIBUTE = "method";

	private static final String DEFS_ELEMENT = "caching";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return CacheInterceptor.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addPropertyReference("cacheManager", CacheNamespaceHandler.extractCacheManager(element));
		CacheNamespaceHandler.parseKeyGenerator(element, builder.getBeanDefinition());

		List<Element> cacheDefs = DomUtils.getChildElementsByTagName(element, DEFS_ELEMENT);
		if (cacheDefs.size() >= 1) {
			// Using attributes source.
			List<RootBeanDefinition> attributeSourceDefinitions = parseDefinitionsSources(cacheDefs, parserContext);
			builder.addPropertyValue("cacheOperationSources", attributeSourceDefinitions);
		} else {
			// Assume annotations source.
			builder.addPropertyValue("cacheOperationSources", new RootBeanDefinition(
					AnnotationCacheOperationSource.class));
		}
	}

	private List<RootBeanDefinition> parseDefinitionsSources(List<Element> definitions, ParserContext parserContext) {
		ManagedList<RootBeanDefinition> defs = new ManagedList<RootBeanDefinition>(definitions.size());

		// extract default param for the definition
		for (Element element : definitions) {
			defs.add(parseDefinitionSource(element, parserContext));
		}

		return defs;
	}

	private RootBeanDefinition parseDefinitionSource(Element definition, ParserContext parserContext) {
		Props prop = new Props(definition);
		// add cacheable first

		ManagedMap<TypedStringValue, Collection<CacheOperation>> cacheOpMap = new ManagedMap<TypedStringValue, Collection<CacheOperation>>();
		cacheOpMap.setSource(parserContext.extractSource(definition));

		List<Element> cacheableCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHEABLE_ELEMENT);

		for (Element opElement : cacheableCacheMethods) {
			String name = prop.merge(opElement, parserContext.getReaderContext());
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(opElement));
			CacheableOperation op = prop.merge(opElement, parserContext.getReaderContext(), new CacheableOperation());
			op.setUnless(getAttributeValue(opElement, "unless", ""));

			Collection<CacheOperation> col = cacheOpMap.get(nameHolder);
			if (col == null) {
				col = new ArrayList<CacheOperation>(2);
				cacheOpMap.put(nameHolder, col);
			}
			col.add(op);
		}

		List<Element> evictCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHE_EVICT_ELEMENT);

		for (Element opElement : evictCacheMethods) {
			String name = prop.merge(opElement, parserContext.getReaderContext());
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(opElement));
			CacheEvictOperation op = prop.merge(opElement, parserContext.getReaderContext(), new CacheEvictOperation());

			String wide = opElement.getAttribute("all-entries");
			if (StringUtils.hasText(wide)) {
				op.setCacheWide(Boolean.valueOf(wide.trim()));
			}

			String after = opElement.getAttribute("before-invocation");
			if (StringUtils.hasText(after)) {
				op.setBeforeInvocation(Boolean.valueOf(after.trim()));
			}

			Collection<CacheOperation> col = cacheOpMap.get(nameHolder);
			if (col == null) {
				col = new ArrayList<CacheOperation>(2);
				cacheOpMap.put(nameHolder, col);
			}
			col.add(op);
		}

		List<Element> putCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHE_PUT_ELEMENT);

		for (Element opElement : putCacheMethods) {
			String name = prop.merge(opElement, parserContext.getReaderContext());
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(opElement));
			CachePutOperation op = prop.merge(opElement, parserContext.getReaderContext(), new CachePutOperation());
			op.setUnless(getAttributeValue(opElement, "unless", ""));

			Collection<CacheOperation> col = cacheOpMap.get(nameHolder);
			if (col == null) {
				col = new ArrayList<CacheOperation>(2);
				cacheOpMap.put(nameHolder, col);
			}
			col.add(op);
		}

		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(NameMatchCacheOperationSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(definition));
		attributeSourceDefinition.getPropertyValues().add("nameMap", cacheOpMap);
		return attributeSourceDefinition;
	}


	private static String getAttributeValue(Element element, String attributeName, String defaultValue) {
		String attribute = element.getAttribute(attributeName);
		if (StringUtils.hasText(attribute)) {
			return attribute.trim();
		}
		return defaultValue;
	}


	/**
	 * Simple, reusable class used for overriding defaults.
	 *
	 * @author Costin Leau
	 */
	private static class Props {

		private String key;

		private String keyGenerator;

		private String cacheManager;

		private String condition;

		private String method;

		private String[] caches = null;


		Props(Element root) {
			String defaultCache = root.getAttribute("cache");
			key = root.getAttribute("key");
			keyGenerator = root.getAttribute("key-generator");
			cacheManager = root.getAttribute("cache-manager");
			condition = root.getAttribute("condition");
			method = root.getAttribute(METHOD_ATTRIBUTE);

			if (StringUtils.hasText(defaultCache)) {
				caches = StringUtils.commaDelimitedListToStringArray(defaultCache.trim());
			}
		}


		<T extends CacheOperation> T merge(Element element, ReaderContext readerCtx, T op) {
			String cache = element.getAttribute("cache");

			// sanity check
			String[] localCaches = caches;
			if (StringUtils.hasText(cache)) {
				localCaches = StringUtils.commaDelimitedListToStringArray(cache.trim());
			} else {
				if (caches == null) {
					readerCtx.error("No cache specified specified for " + element.getNodeName(), element);
				}
			}
			op.setCacheNames(localCaches);

			op.setKey(getAttributeValue(element, "key", this.key));
			op.setKeyGenerator(getAttributeValue(element, "key-generator", this.keyGenerator));
			op.setCacheManager(getAttributeValue(element, "cache-manager", this.cacheManager));
			op.setCondition(getAttributeValue(element, "condition", this.condition));

			if (StringUtils.hasText(op.getKey()) && StringUtils.hasText(op.getKeyGenerator())) {
				throw new IllegalStateException("Invalid cache advice configuration on '"
						+ element.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
						"These attributes are mutually exclusive: either set the SpEL expression used to" +
						"compute the key at runtime or set the name of the KeyGenerator bean to use.");
			}

			return op;
		}

		String merge(Element element, ReaderContext readerCtx) {
			String m = element.getAttribute(METHOD_ATTRIBUTE);

			if (StringUtils.hasText(m)) {
				return m.trim();
			}
			if (StringUtils.hasText(method)) {
				return method;
			}
			readerCtx.error("No method specified for " + element.getNodeName(), element);
			return null;
		}
	}
}
