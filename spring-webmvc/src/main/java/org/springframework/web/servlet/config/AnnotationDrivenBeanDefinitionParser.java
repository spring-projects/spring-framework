/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.Properties;

import org.w3c.dom.Element;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewRequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletWebArgumentResolverAdapter;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * A {@link BeanDefinitionParser} that provides the configuration for the
 * {@code <annotation-driven/>} MVC namespace element.
 *
 * <p>This class registers the following {@link HandlerMapping}s:</p>
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}
 * ordered at 0 for mapping requests to annotated controller methods.
 * <li>{@link BeanNameUrlHandlerMapping}
 * ordered at 2 to map URL paths to controller bean names.
 * </ul>
 *
 * <p><strong>Note:</strong> Additional HandlerMappings may be registered
 * as a result of using the {@code <view-controller>} or the
 * {@code <resources>} MVC namespace elements.
 *
 * <p>This class registers the following {@link HandlerAdapter}s:
 * <ul>
 * <li>{@link RequestMappingHandlerAdapter}
 * for processing requests with annotated controller methods.
 * <li>{@link HttpRequestHandlerAdapter}
 * for processing requests with {@link HttpRequestHandler}s.
 * <li>{@link SimpleControllerHandlerAdapter}
 * for processing requests with interface-based {@link Controller}s.
 * </ul>
 *
 * <p>This class registers the following {@link HandlerExceptionResolver}s:
 * <ul>
 * <li>{@link ExceptionHandlerExceptionResolver} for handling exceptions
 * through @{@link ExceptionHandler} methods.
 * <li>{@link ResponseStatusExceptionResolver} for exceptions annotated
 * with @{@link ResponseStatus}.
 * <li>{@link DefaultHandlerExceptionResolver} for resolving known Spring
 * exception types
 * </ul>
 *
 * <p>This class registers an {@link org.springframework.util.AntPathMatcher}
 * and a {@link org.springframework.web.util.UrlPathHelper} to be used by:
 * <ul>
 * <li>the {@link RequestMappingHandlerMapping},
 * <li>the {@link HandlerMapping} for ViewControllers
 * <li>and the {@link HandlerMapping} for serving resources
 * </ul>
 * Note that those beans can be configured by using the {@code path-matching} MVC namespace element.
 *
 * <p>Both the {@link RequestMappingHandlerAdapter} and the
 * {@link ExceptionHandlerExceptionResolver} are configured with instances of
 * the following by default:
 * <ul>
 * <li>A {@link ContentNegotiationManager}
 * <li>A {@link DefaultFormattingConversionService}
 * <li>A {@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}
 * if a JSR-303 implementation is available on the classpath
 * <li>A range of {@link HttpMessageConverter}s depending on what 3rd party
 * libraries are available on the classpath.
 * </ul>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Agim Emruli
 * @since 3.0
 */
class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

	public static final String HANDLER_MAPPING_BEAN_NAME = RequestMappingHandlerMapping.class.getName();

	public static final String HANDLER_ADAPTER_BEAN_NAME = RequestMappingHandlerAdapter.class.getName();

	public static final String CONTENT_NEGOTIATION_MANAGER_BEAN_NAME = "mvcContentNegotiationManager";

	private static final boolean javaxValidationPresent =
			ClassUtils.isPresent("javax.validation.Validator", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static boolean romePresent =
			ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", AnnotationDrivenBeanDefinitionParser.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jackson2XmlPresent =
			ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean gsonPresent =
			ClassUtils.isPresent("com.google.gson.Gson", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());


	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		XmlReaderContext readerContext = parserContext.getReaderContext();

		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		parserContext.pushContainingComponent(compDefinition);

		RuntimeBeanReference contentNegotiationManager = getContentNegotiationManager(element, source, parserContext);

		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(RequestMappingHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerMappingDef.getPropertyValues().add("order", 0);
		handlerMappingDef.getPropertyValues().add("contentNegotiationManager", contentNegotiationManager);

		if (element.hasAttribute("enable-matrix-variables")) {
			Boolean enableMatrixVariables = Boolean.valueOf(element.getAttribute("enable-matrix-variables"));
			handlerMappingDef.getPropertyValues().add("removeSemicolonContent", !enableMatrixVariables);
		}

		configurePathMatchingProperties(handlerMappingDef, element, parserContext);
		readerContext.getRegistry().registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME , handlerMappingDef);

		RuntimeBeanReference corsConfigurationsRef = MvcNamespaceUtils.registerCorsConfigurations(null, parserContext, source);
		handlerMappingDef.getPropertyValues().add("corsConfigurations", corsConfigurationsRef);

		RuntimeBeanReference conversionService = getConversionService(element, source, parserContext);
		RuntimeBeanReference validator = getValidator(element, source, parserContext);
		RuntimeBeanReference messageCodesResolver = getMessageCodesResolver(element);

		RootBeanDefinition bindingDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
		bindingDef.setSource(source);
		bindingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		bindingDef.getPropertyValues().add("conversionService", conversionService);
		bindingDef.getPropertyValues().add("validator", validator);
		bindingDef.getPropertyValues().add("messageCodesResolver", messageCodesResolver);

		ManagedList<?> messageConverters = getMessageConverters(element, source, parserContext);
		ManagedList<?> argumentResolvers = getArgumentResolvers(element, parserContext);
		ManagedList<?> returnValueHandlers = getReturnValueHandlers(element, parserContext);
		String asyncTimeout = getAsyncTimeout(element);
		RuntimeBeanReference asyncExecutor = getAsyncExecutor(element);
		ManagedList<?> callableInterceptors = getCallableInterceptors(element, source, parserContext);
		ManagedList<?> deferredResultInterceptors = getDeferredResultInterceptors(element, source, parserContext);

		RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
		handlerAdapterDef.setSource(source);
		handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerAdapterDef.getPropertyValues().add("contentNegotiationManager", contentNegotiationManager);
		handlerAdapterDef.getPropertyValues().add("webBindingInitializer", bindingDef);
		handlerAdapterDef.getPropertyValues().add("messageConverters", messageConverters);
		addRequestBodyAdvice(handlerAdapterDef);
		addResponseBodyAdvice(handlerAdapterDef);

		if (element.hasAttribute("ignore-default-model-on-redirect")) {
			Boolean ignoreDefaultModel = Boolean.valueOf(element.getAttribute("ignore-default-model-on-redirect"));
			handlerAdapterDef.getPropertyValues().add("ignoreDefaultModelOnRedirect", ignoreDefaultModel);
		}
		if (argumentResolvers != null) {
			handlerAdapterDef.getPropertyValues().add("customArgumentResolvers", argumentResolvers);
		}
		if (returnValueHandlers != null) {
			handlerAdapterDef.getPropertyValues().add("customReturnValueHandlers", returnValueHandlers);
		}
		if (asyncTimeout != null) {
			handlerAdapterDef.getPropertyValues().add("asyncRequestTimeout", asyncTimeout);
		}
		if (asyncExecutor != null) {
			handlerAdapterDef.getPropertyValues().add("taskExecutor", asyncExecutor);
		}

		handlerAdapterDef.getPropertyValues().add("callableInterceptors", callableInterceptors);
		handlerAdapterDef.getPropertyValues().add("deferredResultInterceptors", deferredResultInterceptors);
		readerContext.getRegistry().registerBeanDefinition(HANDLER_ADAPTER_BEAN_NAME , handlerAdapterDef);

		String uriCompContribName = MvcUriComponentsBuilder.MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME;
		RootBeanDefinition uriCompContribDef = new RootBeanDefinition(CompositeUriComponentsContributorFactoryBean.class);
		uriCompContribDef.setSource(source);
		uriCompContribDef.getPropertyValues().addPropertyValue("handlerAdapter", handlerAdapterDef);
		uriCompContribDef.getPropertyValues().addPropertyValue("conversionService", conversionService);
		readerContext.getRegistry().registerBeanDefinition(uriCompContribName, uriCompContribDef);

		RootBeanDefinition csInterceptorDef = new RootBeanDefinition(ConversionServiceExposingInterceptor.class);
		csInterceptorDef.setSource(source);
		csInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, conversionService);
		RootBeanDefinition mappedCsInterceptorDef = new RootBeanDefinition(MappedInterceptor.class);
		mappedCsInterceptorDef.setSource(source);
		mappedCsInterceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		mappedCsInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, (Object) null);
		mappedCsInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(1, csInterceptorDef);
		String mappedInterceptorName = readerContext.registerWithGeneratedName(mappedCsInterceptorDef);

		RootBeanDefinition exceptionHandlerExceptionResolver = new RootBeanDefinition(ExceptionHandlerExceptionResolver.class);
		exceptionHandlerExceptionResolver.setSource(source);
		exceptionHandlerExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		exceptionHandlerExceptionResolver.getPropertyValues().add("contentNegotiationManager", contentNegotiationManager);
		exceptionHandlerExceptionResolver.getPropertyValues().add("messageConverters", messageConverters);
		exceptionHandlerExceptionResolver.getPropertyValues().add("order", 0);
		addResponseBodyAdvice(exceptionHandlerExceptionResolver);

		if (argumentResolvers != null) {
			exceptionHandlerExceptionResolver.getPropertyValues().add("customArgumentResolvers", argumentResolvers);
		}
		if (returnValueHandlers != null) {
			exceptionHandlerExceptionResolver.getPropertyValues().add("customReturnValueHandlers", returnValueHandlers);
		}

		String methodExceptionResolverName = readerContext.registerWithGeneratedName(exceptionHandlerExceptionResolver);

		RootBeanDefinition responseStatusExceptionResolver = new RootBeanDefinition(ResponseStatusExceptionResolver.class);
		responseStatusExceptionResolver.setSource(source);
		responseStatusExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		responseStatusExceptionResolver.getPropertyValues().add("order", 1);
		String responseStatusExceptionResolverName =
				readerContext.registerWithGeneratedName(responseStatusExceptionResolver);

		RootBeanDefinition defaultExceptionResolver = new RootBeanDefinition(DefaultHandlerExceptionResolver.class);
		defaultExceptionResolver.setSource(source);
		defaultExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		defaultExceptionResolver.getPropertyValues().add("order", 2);
		String defaultExceptionResolverName =
				readerContext.registerWithGeneratedName(defaultExceptionResolver);

		parserContext.registerComponent(new BeanComponentDefinition(handlerMappingDef, HANDLER_MAPPING_BEAN_NAME));
		parserContext.registerComponent(new BeanComponentDefinition(handlerAdapterDef, HANDLER_ADAPTER_BEAN_NAME));
		parserContext.registerComponent(new BeanComponentDefinition(uriCompContribDef, uriCompContribName));
		parserContext.registerComponent(new BeanComponentDefinition(exceptionHandlerExceptionResolver, methodExceptionResolverName));
		parserContext.registerComponent(new BeanComponentDefinition(responseStatusExceptionResolver, responseStatusExceptionResolverName));
		parserContext.registerComponent(new BeanComponentDefinition(defaultExceptionResolver, defaultExceptionResolverName));
		parserContext.registerComponent(new BeanComponentDefinition(mappedCsInterceptorDef, mappedInterceptorName));

		// Ensure BeanNameUrlHandlerMapping (SPR-8289) and default HandlerAdapters are not "turned off"
		MvcNamespaceUtils.registerDefaultComponents(parserContext, source);

		parserContext.popAndRegisterContainingComponent();

		return null;
	}

	protected void addRequestBodyAdvice(RootBeanDefinition beanDef) {
		if (jackson2Present) {
			beanDef.getPropertyValues().add("requestBodyAdvice",
					new RootBeanDefinition(JsonViewRequestBodyAdvice.class));
		}
	}

	protected void addResponseBodyAdvice(RootBeanDefinition beanDef) {
		if (jackson2Present) {
			beanDef.getPropertyValues().add("responseBodyAdvice",
					new RootBeanDefinition(JsonViewResponseBodyAdvice.class));
		}
	}

	private RuntimeBeanReference getConversionService(Element element, Object source, ParserContext parserContext) {
		RuntimeBeanReference conversionServiceRef;
		if (element.hasAttribute("conversion-service")) {
			conversionServiceRef = new RuntimeBeanReference(element.getAttribute("conversion-service"));
		}
		else {
			RootBeanDefinition conversionDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
			conversionDef.setSource(source);
			conversionDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String conversionName = parserContext.getReaderContext().registerWithGeneratedName(conversionDef);
			parserContext.registerComponent(new BeanComponentDefinition(conversionDef, conversionName));
			conversionServiceRef = new RuntimeBeanReference(conversionName);
		}
		return conversionServiceRef;
	}

	private RuntimeBeanReference getValidator(Element element, Object source, ParserContext parserContext) {
		if (element.hasAttribute("validator")) {
			return new RuntimeBeanReference(element.getAttribute("validator"));
		}
		else if (javaxValidationPresent) {
			RootBeanDefinition validatorDef = new RootBeanDefinition(
					"org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean");
			validatorDef.setSource(source);
			validatorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String validatorName = parserContext.getReaderContext().registerWithGeneratedName(validatorDef);
			parserContext.registerComponent(new BeanComponentDefinition(validatorDef, validatorName));
			return new RuntimeBeanReference(validatorName);
		}
		else {
			return null;
		}
	}

	private RuntimeBeanReference getContentNegotiationManager(Element element, Object source,
			ParserContext parserContext) {

		RuntimeBeanReference beanRef;
		if (element.hasAttribute("content-negotiation-manager")) {
			String name = element.getAttribute("content-negotiation-manager");
			beanRef = new RuntimeBeanReference(name);
		}
		else {
			RootBeanDefinition factoryBeanDef = new RootBeanDefinition(ContentNegotiationManagerFactoryBean.class);
			factoryBeanDef.setSource(source);
			factoryBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			factoryBeanDef.getPropertyValues().add("mediaTypes", getDefaultMediaTypes());

			String name = CONTENT_NEGOTIATION_MANAGER_BEAN_NAME;
			parserContext.getReaderContext().getRegistry().registerBeanDefinition(name , factoryBeanDef);
			parserContext.registerComponent(new BeanComponentDefinition(factoryBeanDef, name));
			beanRef = new RuntimeBeanReference(name);
		}
		return beanRef;
	}

	private void configurePathMatchingProperties(RootBeanDefinition handlerMappingDef, Element element,
			ParserContext parserContext) {

		Element pathMatchingElement = DomUtils.getChildElementByTagName(element, "path-matching");
		if (pathMatchingElement != null) {
			Object source = parserContext.extractSource(element);
			if (pathMatchingElement.hasAttribute("suffix-pattern")) {
				Boolean useSuffixPatternMatch = Boolean.valueOf(pathMatchingElement.getAttribute("suffix-pattern"));
				handlerMappingDef.getPropertyValues().add("useSuffixPatternMatch", useSuffixPatternMatch);
			}
			if (pathMatchingElement.hasAttribute("trailing-slash")) {
				Boolean useTrailingSlashMatch = Boolean.valueOf(pathMatchingElement.getAttribute("trailing-slash"));
				handlerMappingDef.getPropertyValues().add("useTrailingSlashMatch", useTrailingSlashMatch);
			}
			if (pathMatchingElement.hasAttribute("registered-suffixes-only")) {
				Boolean useRegisteredSuffixPatternMatch = Boolean.valueOf(pathMatchingElement.getAttribute("registered-suffixes-only"));
				handlerMappingDef.getPropertyValues().add("useRegisteredSuffixPatternMatch", useRegisteredSuffixPatternMatch);
			}
			RuntimeBeanReference pathHelperRef = null;
			if (pathMatchingElement.hasAttribute("path-helper")) {
				pathHelperRef = new RuntimeBeanReference(pathMatchingElement.getAttribute("path-helper"));
			}
			pathHelperRef = MvcNamespaceUtils.registerUrlPathHelper(pathHelperRef, parserContext, source);
			handlerMappingDef.getPropertyValues().add("urlPathHelper", pathHelperRef);

			RuntimeBeanReference pathMatcherRef = null;
			if (pathMatchingElement.hasAttribute("path-matcher")) {
				pathMatcherRef = new RuntimeBeanReference(pathMatchingElement.getAttribute("path-matcher"));
			}
			pathMatcherRef = MvcNamespaceUtils.registerPathMatcher(pathMatcherRef, parserContext, source);
			handlerMappingDef.getPropertyValues().add("pathMatcher", pathMatcherRef);
		}
	}

	private Properties getDefaultMediaTypes() {
		Properties props = new Properties();
		if (romePresent) {
			props.put("atom", MediaType.APPLICATION_ATOM_XML_VALUE);
			props.put("rss", "application/rss+xml");
		}
		if (jaxb2Present || jackson2XmlPresent) {
			props.put("xml", MediaType.APPLICATION_XML_VALUE);
		}
		if (jackson2Present || gsonPresent) {
			props.put("json", MediaType.APPLICATION_JSON_VALUE);
		}
		return props;
	}

	private RuntimeBeanReference getMessageCodesResolver(Element element) {
		if (element.hasAttribute("message-codes-resolver")) {
			return new RuntimeBeanReference(element.getAttribute("message-codes-resolver"));
		}
		else {
			return null;
		}
	}

	private String getAsyncTimeout(Element element) {
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		return (asyncElement != null) ? asyncElement.getAttribute("default-timeout") : null;
	}

	private RuntimeBeanReference getAsyncExecutor(Element element) {
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		if (asyncElement != null) {
			if (asyncElement.hasAttribute("task-executor")) {
				return new RuntimeBeanReference(asyncElement.getAttribute("task-executor"));
			}
		}
		return null;
	}

	private ManagedList<?> getCallableInterceptors(Element element, Object source, ParserContext parserContext) {
		ManagedList<? super Object> interceptors = new ManagedList<>();
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		if (asyncElement != null) {
			Element interceptorsElement = DomUtils.getChildElementByTagName(asyncElement, "callable-interceptors");
			if (interceptorsElement != null) {
				interceptors.setSource(source);
				for (Element converter : DomUtils.getChildElementsByTagName(interceptorsElement, "bean")) {
					BeanDefinitionHolder beanDef = parserContext.getDelegate().parseBeanDefinitionElement(converter);
					beanDef = parserContext.getDelegate().decorateBeanDefinitionIfRequired(converter, beanDef);
					interceptors.add(beanDef);
				}
			}
		}
		return interceptors;
	}

	private ManagedList<?> getDeferredResultInterceptors(Element element, Object source, ParserContext parserContext) {
		ManagedList<? super Object> interceptors = new ManagedList<>();
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		if (asyncElement != null) {
			Element interceptorsElement = DomUtils.getChildElementByTagName(asyncElement, "deferred-result-interceptors");
			if (interceptorsElement != null) {
				interceptors.setSource(source);
				for (Element converter : DomUtils.getChildElementsByTagName(interceptorsElement, "bean")) {
					BeanDefinitionHolder beanDef = parserContext.getDelegate().parseBeanDefinitionElement(converter);
					beanDef = parserContext.getDelegate().decorateBeanDefinitionIfRequired(converter, beanDef);
					interceptors.add(beanDef);
				}
			}
		}
		return interceptors;
	}

	private ManagedList<?> getArgumentResolvers(Element element, ParserContext parserContext) {
		Element resolversElement = DomUtils.getChildElementByTagName(element, "argument-resolvers");
		if (resolversElement != null) {
			ManagedList<Object> resolvers = extractBeanSubElements(resolversElement, parserContext);
			return wrapLegacyResolvers(resolvers, parserContext);
		}
		return null;
	}

	private ManagedList<Object> wrapLegacyResolvers(List<Object> list, ParserContext context) {
		ManagedList<Object> result = new ManagedList<>();
		for (Object object : list) {
			if (object instanceof BeanDefinitionHolder) {
				BeanDefinitionHolder beanDef = (BeanDefinitionHolder) object;
				String className = beanDef.getBeanDefinition().getBeanClassName();
				Class<?> clazz = ClassUtils.resolveClassName(className, context.getReaderContext().getBeanClassLoader());
				if (WebArgumentResolver.class.isAssignableFrom(clazz)) {
					RootBeanDefinition adapter = new RootBeanDefinition(ServletWebArgumentResolverAdapter.class);
					adapter.getConstructorArgumentValues().addIndexedArgumentValue(0, beanDef);
					result.add(new BeanDefinitionHolder(adapter, beanDef.getBeanName() + "Adapter"));
					continue;
				}
			}
			result.add(object);
		}
		return result;
	}

	private ManagedList<?> getReturnValueHandlers(Element element, ParserContext parserContext) {
		Element handlers = DomUtils.getChildElementByTagName(element, "return-value-handlers");
		return (handlers != null ? extractBeanSubElements(handlers, parserContext) : null);
	}

	private ManagedList<?> getMessageConverters(Element element, Object source, ParserContext parserContext) {
		Element convertersElement = DomUtils.getChildElementByTagName(element, "message-converters");
		ManagedList<? super Object> messageConverters = new ManagedList<>();
		if (convertersElement != null) {
			messageConverters.setSource(source);
			for (Element beanElement : DomUtils.getChildElementsByTagName(convertersElement, "bean", "ref")) {
				Object object = parserContext.getDelegate().parsePropertySubElement(beanElement, null);
				messageConverters.add(object);
			}
		}

		if (convertersElement == null || Boolean.valueOf(convertersElement.getAttribute("register-defaults"))) {
			messageConverters.setSource(source);
			messageConverters.add(createConverterDefinition(ByteArrayHttpMessageConverter.class, source));

			RootBeanDefinition stringConverterDef = createConverterDefinition(StringHttpMessageConverter.class, source);
			stringConverterDef.getPropertyValues().add("writeAcceptCharset", false);
			messageConverters.add(stringConverterDef);

			messageConverters.add(createConverterDefinition(ResourceHttpMessageConverter.class, source));
			messageConverters.add(createConverterDefinition(SourceHttpMessageConverter.class, source));
			messageConverters.add(createConverterDefinition(AllEncompassingFormHttpMessageConverter.class, source));

			if (romePresent) {
				messageConverters.add(createConverterDefinition(AtomFeedHttpMessageConverter.class, source));
				messageConverters.add(createConverterDefinition(RssChannelHttpMessageConverter.class, source));
			}

			if (jackson2XmlPresent) {
				RootBeanDefinition jacksonConverterDef = createConverterDefinition(MappingJackson2XmlHttpMessageConverter.class, source);
				GenericBeanDefinition jacksonFactoryDef = createObjectMapperFactoryDefinition(source);
				jacksonFactoryDef.getPropertyValues().add("createXmlMapper", true);
				jacksonConverterDef.getConstructorArgumentValues().addIndexedArgumentValue(0, jacksonFactoryDef);
				messageConverters.add(jacksonConverterDef);
			}
			else if (jaxb2Present) {
				messageConverters.add(createConverterDefinition(Jaxb2RootElementHttpMessageConverter.class, source));
			}

			if (jackson2Present) {
				RootBeanDefinition jacksonConverterDef = createConverterDefinition(MappingJackson2HttpMessageConverter.class, source);
				GenericBeanDefinition jacksonFactoryDef = createObjectMapperFactoryDefinition(source);
				jacksonConverterDef.getConstructorArgumentValues().addIndexedArgumentValue(0, jacksonFactoryDef);
				messageConverters.add(jacksonConverterDef);
			}
			else if (gsonPresent) {
				messageConverters.add(createConverterDefinition(GsonHttpMessageConverter.class, source));
			}
		}
		return messageConverters;
	}

	private GenericBeanDefinition createObjectMapperFactoryDefinition(Object source) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(Jackson2ObjectMapperFactoryBean.class);
		beanDefinition.setSource(source);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		return beanDefinition;
	}

	private RootBeanDefinition createConverterDefinition(Class<?> converterClass, Object source) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(converterClass);
		beanDefinition.setSource(source);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		return beanDefinition;
	}


	private ManagedList<Object> extractBeanSubElements(Element parentElement, ParserContext parserContext) {
		ManagedList<Object> list = new ManagedList<>();
		list.setSource(parserContext.extractSource(parentElement));
		for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, "bean", "ref")) {
			Object object = parserContext.getDelegate().parsePropertySubElement(beanElement, null);
			list.add(object);
		}
		return list;
	}

    private ManagedList<BeanReference> extractBeanRefSubElements(Element parentElement, ParserContext parserContext){
        ManagedList<BeanReference> list = new ManagedList<>();
        list.setSource(parserContext.extractSource(parentElement));
        for (Element refElement : DomUtils.getChildElementsByTagName(parentElement, "ref")) {
            BeanReference reference;
            if (StringUtils.hasText("bean")) {
                reference = new RuntimeBeanReference(refElement.getAttribute("bean"),false);
                list.add(reference);
            }
			else if (StringUtils.hasText("parent")){
                reference = new RuntimeBeanReference(refElement.getAttribute("parent"),true);
                list.add(reference);
            }
			else {
                parserContext.getReaderContext().error("'bean' or 'parent' attribute is required for <ref> element",
                        parserContext.extractSource(parentElement));
            }
        }
        return list;
    }


	/**
	 * A FactoryBean for a CompositeUriComponentsContributor that obtains the
	 * HandlerMethodArgumentResolver's configured in RequestMappingHandlerAdapter
	 * after it is fully initialized.
	 */
	static class CompositeUriComponentsContributorFactoryBean
			implements FactoryBean<CompositeUriComponentsContributor>, InitializingBean {

		private RequestMappingHandlerAdapter handlerAdapter;

		private ConversionService conversionService;

		private CompositeUriComponentsContributor uriComponentsContributor;

		public void setHandlerAdapter(RequestMappingHandlerAdapter handlerAdapter) {
			this.handlerAdapter = handlerAdapter;
		}

		public void setConversionService(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		@Override
		public void afterPropertiesSet() {
			this.uriComponentsContributor = new CompositeUriComponentsContributor(
					this.handlerAdapter.getArgumentResolvers(), this.conversionService);
		}

		@Override
		public CompositeUriComponentsContributor getObject() throws Exception {
			return this.uriComponentsContributor;
		}

		@Override
		public Class<?> getObjectType() {
			return CompositeUriComponentsContributor.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

}
