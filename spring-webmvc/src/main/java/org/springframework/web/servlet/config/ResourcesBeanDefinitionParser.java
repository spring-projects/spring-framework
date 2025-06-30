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
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.ContentVersionStrategy;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.FixedVersionStrategy;
import org.springframework.web.servlet.resource.LiteWebJarsResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.resource.VersionResourceResolver;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a
 * {@code resources} element to register a {@link ResourceHttpRequestHandler} and
 * register a {@link SimpleUrlHandlerMapping} for mapping resource requests, and a
 * {@link HttpRequestHandlerAdapter}. Will also create a resource handling chain with
 * {@link ResourceResolver ResourceResolvers} and {@link ResourceTransformer ResourceTransformers}.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Brian Clozel
 * @since 3.0.4
 */
class ResourcesBeanDefinitionParser implements BeanDefinitionParser {

	private static final String RESOURCE_CHAIN_CACHE = "spring-resource-chain-cache";

	private static final String VERSION_RESOLVER_ELEMENT = "version-resolver";

	private static final String VERSION_STRATEGY_ELEMENT = "version-strategy";

	private static final String FIXED_VERSION_STRATEGY_ELEMENT = "fixed-version-strategy";

	private static final String CONTENT_VERSION_STRATEGY_ELEMENT = "content-version-strategy";

	private static final String RESOURCE_URL_PROVIDER = "mvcResourceUrlProvider";

	private static final boolean webJarsPresent = ClassUtils.isPresent(
			"org.webjars.WebJarVersionLocator", ResourcesBeanDefinitionParser.class.getClassLoader());


	@Override
	public @Nullable BeanDefinition parse(Element element, ParserContext context) {
		Object source = context.extractSource(element);

		registerUrlProvider(context, source);

		RuntimeBeanReference pathHelperRef = MvcNamespaceUtils.registerUrlPathHelper(null, context, source);

		String resourceHandlerName = registerResourceHandler(context, element, pathHelperRef, source);
		if (resourceHandlerName == null) {
			return null;
		}

		Map<String, String> urlMap = new ManagedMap<>();
		String resourceRequestPath = element.getAttribute("mapping");
		if (!StringUtils.hasText(resourceRequestPath)) {
			context.getReaderContext().error("The 'mapping' attribute is required.", context.extractSource(element));
			return null;
		}
		urlMap.put(resourceRequestPath, resourceHandlerName);

		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap).add("urlPathHelper", pathHelperRef);
		MvcNamespaceUtils.configurePathMatching(handlerMappingDef, context, source);

		String orderValue = element.getAttribute("order");
		// Use a default of near-lowest precedence, still allowing for even lower precedence in other mappings
		Object order = StringUtils.hasText(orderValue) ? orderValue : Ordered.LOWEST_PRECEDENCE - 1;
		handlerMappingDef.getPropertyValues().add("order", order);

		RuntimeBeanReference corsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
		handlerMappingDef.getPropertyValues().add("corsConfigurations", corsRef);

		String beanName = context.getReaderContext().generateBeanName(handlerMappingDef);
		context.getRegistry().registerBeanDefinition(beanName, handlerMappingDef);
		context.registerComponent(new BeanComponentDefinition(handlerMappingDef, beanName));

		// Ensure BeanNameUrlHandlerMapping (SPR-8289) and default HandlerAdapters are not "turned off"
		// Register HttpRequestHandlerAdapter
		MvcNamespaceUtils.registerDefaultComponents(context, source);

		return null;
	}

	private void registerUrlProvider(ParserContext context, @Nullable Object source) {
		if (!context.getRegistry().containsBeanDefinition(RESOURCE_URL_PROVIDER)) {
			RootBeanDefinition urlProvider = new RootBeanDefinition(ResourceUrlProvider.class);
			urlProvider.setSource(source);
			urlProvider.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			context.getRegistry().registerBeanDefinition(RESOURCE_URL_PROVIDER, urlProvider);
			context.registerComponent(new BeanComponentDefinition(urlProvider, RESOURCE_URL_PROVIDER));

			RootBeanDefinition interceptor = new RootBeanDefinition(ResourceUrlProviderExposingInterceptor.class);
			interceptor.setSource(source);
			interceptor.getConstructorArgumentValues().addIndexedArgumentValue(0, urlProvider);

			RootBeanDefinition mappedInterceptor = new RootBeanDefinition(MappedInterceptor.class);
			mappedInterceptor.setSource(source);
			mappedInterceptor.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			mappedInterceptor.getConstructorArgumentValues().addIndexedArgumentValue(0, (Object) null);
			mappedInterceptor.getConstructorArgumentValues().addIndexedArgumentValue(1, interceptor);
			String mappedInterceptorName = context.getReaderContext().registerWithGeneratedName(mappedInterceptor);
			context.registerComponent(new BeanComponentDefinition(mappedInterceptor, mappedInterceptorName));
		}
	}

	private @Nullable String registerResourceHandler(ParserContext context, Element element,
			RuntimeBeanReference pathHelperRef, @Nullable Object source) {

		String locationAttr = element.getAttribute("location");
		if (!StringUtils.hasText(locationAttr)) {
			context.getReaderContext().error("The 'location' attribute is required.", context.extractSource(element));
			return null;
		}

		RootBeanDefinition resourceHandlerDef = new RootBeanDefinition(ResourceHttpRequestHandler.class);
		resourceHandlerDef.setSource(source);
		resourceHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		MutablePropertyValues values = resourceHandlerDef.getPropertyValues();
		values.add("urlPathHelper", pathHelperRef);
		values.add("locationValues", StringUtils.commaDelimitedListToStringArray(locationAttr));

		String cacheSeconds = element.getAttribute("cache-period");
		if (StringUtils.hasText(cacheSeconds)) {
			values.add("cacheSeconds", cacheSeconds);
		}

		Element cacheControlElement = DomUtils.getChildElementByTagName(element, "cache-control");
		if (cacheControlElement != null) {
			CacheControl cacheControl = parseCacheControl(cacheControlElement);
			values.add("cacheControl", cacheControl);
		}

		Element resourceChainElement = DomUtils.getChildElementByTagName(element, "resource-chain");
		if (resourceChainElement != null) {
			parseResourceChain(resourceHandlerDef, context, resourceChainElement, source);
		}

		String beanName = context.getReaderContext().generateBeanName(resourceHandlerDef);
		context.getRegistry().registerBeanDefinition(beanName, resourceHandlerDef);
		context.registerComponent(new BeanComponentDefinition(resourceHandlerDef, beanName));
		return beanName;
	}


	private CacheControl parseCacheControl(Element element) {
		CacheControl cacheControl;
		if ("true".equals(element.getAttribute("no-cache"))) {
			cacheControl = CacheControl.noCache();
		}
		else if ("true".equals(element.getAttribute("no-store"))) {
			cacheControl = CacheControl.noStore();
		}
		else if (element.hasAttribute("max-age")) {
			cacheControl = CacheControl.maxAge(Long.parseLong(element.getAttribute("max-age")), TimeUnit.SECONDS);
		}
		else {
			cacheControl = CacheControl.empty();
		}

		if ("true".equals(element.getAttribute("must-revalidate"))) {
			cacheControl = cacheControl.mustRevalidate();
		}
		if ("true".equals(element.getAttribute("no-transform"))) {
			cacheControl = cacheControl.noTransform();
		}
		if ("true".equals(element.getAttribute("cache-public"))) {
			cacheControl = cacheControl.cachePublic();
		}
		if ("true".equals(element.getAttribute("cache-private"))) {
			cacheControl = cacheControl.cachePrivate();
		}
		if ("true".equals(element.getAttribute("proxy-revalidate"))) {
			cacheControl = cacheControl.proxyRevalidate();
		}
		if (element.hasAttribute("s-maxage")) {
			cacheControl = cacheControl.sMaxAge(Long.parseLong(element.getAttribute("s-maxage")), TimeUnit.SECONDS);
		}
		if (element.hasAttribute("stale-while-revalidate")) {
			cacheControl = cacheControl.staleWhileRevalidate(
					Long.parseLong(element.getAttribute("stale-while-revalidate")), TimeUnit.SECONDS);
		}
		if (element.hasAttribute("stale-if-error")) {
			cacheControl = cacheControl.staleIfError(
					Long.parseLong(element.getAttribute("stale-if-error")), TimeUnit.SECONDS);
		}
		return cacheControl;
	}

	private void parseResourceChain(
			RootBeanDefinition resourceHandlerDef, ParserContext context, Element element, @Nullable Object source) {

		String autoRegistration = element.getAttribute("auto-registration");
		boolean isAutoRegistration = !(StringUtils.hasText(autoRegistration) && "false".equals(autoRegistration));

		ManagedList<Object> resourceResolvers = new ManagedList<>();
		resourceResolvers.setSource(source);
		ManagedList<Object> resourceTransformers = new ManagedList<>();
		resourceTransformers.setSource(source);

		parseResourceCache(resourceResolvers, resourceTransformers, element, source);
		parseResourceResolversTransformers(
				isAutoRegistration, resourceResolvers, resourceTransformers, context, element, source);

		if (!resourceResolvers.isEmpty()) {
			resourceHandlerDef.getPropertyValues().add("resourceResolvers", resourceResolvers);
		}
		if (!resourceTransformers.isEmpty()) {
			resourceHandlerDef.getPropertyValues().add("resourceTransformers", resourceTransformers);
		}
	}

	private void parseResourceCache(ManagedList<Object> resourceResolvers,
			ManagedList<Object> resourceTransformers, Element element, @Nullable Object source) {

		String resourceCache = element.getAttribute("resource-cache");
		if ("true".equals(resourceCache)) {
			ConstructorArgumentValues cargs = new ConstructorArgumentValues();

			RootBeanDefinition cachingResolverDef = new RootBeanDefinition(CachingResourceResolver.class);
			cachingResolverDef.setSource(source);
			cachingResolverDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			cachingResolverDef.setConstructorArgumentValues(cargs);

			RootBeanDefinition cachingTransformerDef = new RootBeanDefinition(CachingResourceTransformer.class);
			cachingTransformerDef.setSource(source);
			cachingTransformerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			cachingTransformerDef.setConstructorArgumentValues(cargs);

			String cacheManagerName = element.getAttribute("cache-manager");
			String cacheName = element.getAttribute("cache-name");
			if (StringUtils.hasText(cacheManagerName) && StringUtils.hasText(cacheName)) {
				RuntimeBeanReference cacheManagerRef = new RuntimeBeanReference(cacheManagerName);
				cargs.addIndexedArgumentValue(0, cacheManagerRef);
				cargs.addIndexedArgumentValue(1, cacheName);
			}
			else {
				ConstructorArgumentValues cacheCavs = new ConstructorArgumentValues();
				cacheCavs.addIndexedArgumentValue(0, RESOURCE_CHAIN_CACHE);
				RootBeanDefinition cacheDef = new RootBeanDefinition(ConcurrentMapCache.class);
				cacheDef.setSource(source);
				cacheDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				cacheDef.setConstructorArgumentValues(cacheCavs);
				cargs.addIndexedArgumentValue(0, cacheDef);
			}
			resourceResolvers.add(cachingResolverDef);
			resourceTransformers.add(cachingTransformerDef);
		}
	}

	@SuppressWarnings("removal")
	private void parseResourceResolversTransformers(boolean isAutoRegistration,
			ManagedList<Object> resourceResolvers, ManagedList<Object> resourceTransformers,
			ParserContext context, Element element, @Nullable Object source) {

		Element resolversElement = DomUtils.getChildElementByTagName(element, "resolvers");
		if (resolversElement != null) {
			for (Element beanElement : DomUtils.getChildElements(resolversElement)) {
				if (VERSION_RESOLVER_ELEMENT.equals(beanElement.getLocalName())) {
					RootBeanDefinition versionResolverDef = parseVersionResolver(context, beanElement, source);
					versionResolverDef.setSource(source);
					resourceResolvers.add(versionResolverDef);
					if (isAutoRegistration) {
						RootBeanDefinition cssLinkTransformerDef = new RootBeanDefinition(CssLinkResourceTransformer.class);
						cssLinkTransformerDef.setSource(source);
						cssLinkTransformerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
						resourceTransformers.add(cssLinkTransformerDef);
					}
				}
				else {
					Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
					resourceResolvers.add(object);
				}
			}
		}

		if (isAutoRegistration) {
			if (webJarsPresent) {
				RootBeanDefinition webJarsResolverDef = new RootBeanDefinition(LiteWebJarsResourceResolver.class);
				webJarsResolverDef.setSource(source);
				webJarsResolverDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				resourceResolvers.add(webJarsResolverDef);
			}
			RootBeanDefinition pathResolverDef = new RootBeanDefinition(PathResourceResolver.class);
			pathResolverDef.setSource(source);
			pathResolverDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			resourceResolvers.add(pathResolverDef);
		}

		Element transformersElement = DomUtils.getChildElementByTagName(element, "transformers");
		if (transformersElement != null) {
			for (Element beanElement : DomUtils.getChildElementsByTagName(transformersElement, "bean", "ref")) {
				Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
				resourceTransformers.add(object);
			}
		}
	}

	private RootBeanDefinition parseVersionResolver(ParserContext context, Element element, @Nullable Object source) {
		ManagedMap<String, Object> strategyMap = new ManagedMap<>();
		strategyMap.setSource(source);
		RootBeanDefinition versionResolverDef = new RootBeanDefinition(VersionResourceResolver.class);
		versionResolverDef.setSource(source);
		versionResolverDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		versionResolverDef.getPropertyValues().addPropertyValue("strategyMap", strategyMap);

		for (Element beanElement : DomUtils.getChildElements(element)) {
			String[] patterns = StringUtils.commaDelimitedListToStringArray(beanElement.getAttribute("patterns"));
			Object strategy = null;
			if (FIXED_VERSION_STRATEGY_ELEMENT.equals(beanElement.getLocalName())) {
				ConstructorArgumentValues cargs = new ConstructorArgumentValues();
				cargs.addIndexedArgumentValue(0, beanElement.getAttribute("version"));
				RootBeanDefinition strategyDef = new RootBeanDefinition(FixedVersionStrategy.class);
				strategyDef.setSource(source);
				strategyDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				strategyDef.setConstructorArgumentValues(cargs);
				strategy = strategyDef;
			}
			else if (CONTENT_VERSION_STRATEGY_ELEMENT.equals(beanElement.getLocalName())) {
				RootBeanDefinition strategyDef = new RootBeanDefinition(ContentVersionStrategy.class);
				strategyDef.setSource(source);
				strategyDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				strategy = strategyDef;
			}
			else if (VERSION_STRATEGY_ELEMENT.equals(beanElement.getLocalName())) {
				Element childElement = DomUtils.getChildElementsByTagName(beanElement, "bean", "ref").get(0);
				strategy = context.getDelegate().parsePropertySubElement(childElement, null);
			}
			for (String pattern : patterns) {
				strategyMap.put(pattern, strategy);
			}
		}

		return versionResolverDef;
	}

}
