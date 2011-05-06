/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletWebArgumentResolverAdapter;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} that parses the {@code annotation-driven} element to configure a Spring MVC web
 * application.
 *
 * <p>Responsible for:
 * <ol>
 * <li>Registering a DefaultAnnotationHandlerMapping bean for mapping HTTP Servlet Requests to @Controller methods
 * using @RequestMapping annotations.
 * <li>Registering a AnnotationMethodHandlerAdapter bean for invoking annotated @Controller methods.
 * Will configure the HandlerAdapter's <code>webBindingInitializer</code> property for centrally configuring
 * {@code @Controller} {@code DataBinder} instances:
 * <ul>
 * <li>Configures the conversionService if specified, otherwise defaults to a fresh {@link ConversionService} instance
 * created by the default {@link FormattingConversionServiceFactoryBean}.
 * <li>Configures the validator if specified, otherwise defaults to a fresh {@link Validator} instance created by the
 * default {@link LocalValidatorFactoryBean} <em>if the JSR-303 API is present on the classpath</em>.
 * <li>Configures standard {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverters},
 * including the {@link Jaxb2RootElementHttpMessageConverter} <em>if JAXB2 is present on the classpath</em>, and
 * the {@link MappingJacksonHttpMessageConverter} <em>if Jackson is present on the classpath</em>.
 * </ul>
 * </ol>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

	private static final boolean jsr303Present = ClassUtils.isPresent(
			"javax.validation.Validator", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jacksonPresent =
			ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", AnnotationDrivenBeanDefinitionParser.class.getClassLoader()) &&
					ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static boolean romePresent =
			ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", AnnotationDrivenBeanDefinitionParser.class.getClassLoader());


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		parserContext.pushContainingComponent(compDefinition);

		RootBeanDefinition methodMappingDef = new RootBeanDefinition(RequestMappingHandlerMapping.class);
		methodMappingDef.setSource(source);
		methodMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		methodMappingDef.getPropertyValues().add("order", 0);
		String methodMappingName = parserContext.getReaderContext().registerWithGeneratedName(methodMappingDef);

		RuntimeBeanReference conversionService = getConversionService(element, source, parserContext);
		RuntimeBeanReference validator = getValidator(element, source, parserContext);
		RuntimeBeanReference messageCodesResolver = getMessageCodesResolver(element, source, parserContext);

		RootBeanDefinition bindingDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
		bindingDef.setSource(source);
		bindingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		bindingDef.getPropertyValues().add("conversionService", conversionService);
		bindingDef.getPropertyValues().add("validator", validator);
		bindingDef.getPropertyValues().add("messageCodesResolver", messageCodesResolver);

		ManagedList<?> messageConverters = getMessageConverters(element, source, parserContext);
		ManagedList<?> argumentResolvers = getArgumentResolvers(element, source, parserContext);
		ManagedList<?> returnValueHandlers = getReturnValueHandlers(element, source, parserContext);

		RootBeanDefinition methodAdapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
		methodAdapterDef.setSource(source);
		methodAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		methodAdapterDef.getPropertyValues().add("webBindingInitializer", bindingDef);
		methodAdapterDef.getPropertyValues().add("messageConverters", messageConverters);
		if (argumentResolvers != null) {
			methodAdapterDef.getPropertyValues().add("customArgumentResolvers", argumentResolvers);
		}
		if (returnValueHandlers != null) {
			methodAdapterDef.getPropertyValues().add("customReturnValueHandlers", returnValueHandlers);
		}
		String methodAdapterName = parserContext.getReaderContext().registerWithGeneratedName(methodAdapterDef);

		RootBeanDefinition csInterceptorDef = new RootBeanDefinition(ConversionServiceExposingInterceptor.class);
		csInterceptorDef.setSource(source);
		csInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, conversionService);
		RootBeanDefinition mappedCsInterceptorDef = new RootBeanDefinition(MappedInterceptor.class);
		mappedCsInterceptorDef.setSource(source);
		mappedCsInterceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		mappedCsInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, (Object) null);
		mappedCsInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(1, csInterceptorDef);
		String mappedInterceptorName = parserContext.getReaderContext().registerWithGeneratedName(mappedCsInterceptorDef);

		RootBeanDefinition methodExceptionResolver = new RootBeanDefinition(ExceptionHandlerExceptionResolver.class);
		methodExceptionResolver.setSource(source);
		methodExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		methodExceptionResolver.getPropertyValues().add("messageConverters", messageConverters);
		methodExceptionResolver.getPropertyValues().add("order", 0);
		String methodExceptionResolverName =
				parserContext.getReaderContext().registerWithGeneratedName(methodExceptionResolver);

		RootBeanDefinition responseStatusExceptionResolver = new RootBeanDefinition(ResponseStatusExceptionResolver.class);
		responseStatusExceptionResolver.setSource(source);
		responseStatusExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		responseStatusExceptionResolver.getPropertyValues().add("order", 1);
		String responseStatusExceptionResolverName =
				parserContext.getReaderContext().registerWithGeneratedName(responseStatusExceptionResolver);

		RootBeanDefinition defaultExceptionResolver = new RootBeanDefinition(DefaultHandlerExceptionResolver.class);
		defaultExceptionResolver.setSource(source);
		defaultExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		defaultExceptionResolver.getPropertyValues().add("order", 2);
		String defaultExceptionResolverName =
				parserContext.getReaderContext().registerWithGeneratedName(defaultExceptionResolver);

		parserContext.registerComponent(new BeanComponentDefinition(methodMappingDef, methodMappingName));
		parserContext.registerComponent(new BeanComponentDefinition(methodAdapterDef, methodAdapterName));
		parserContext.registerComponent(new BeanComponentDefinition(methodExceptionResolver, methodExceptionResolverName));
		parserContext.registerComponent(new BeanComponentDefinition(responseStatusExceptionResolver, responseStatusExceptionResolverName));
		parserContext.registerComponent(new BeanComponentDefinition(defaultExceptionResolver, defaultExceptionResolverName));
		parserContext.registerComponent(new BeanComponentDefinition(mappedCsInterceptorDef, mappedInterceptorName));
		parserContext.popAndRegisterContainingComponent();

		return null;
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
		else if (jsr303Present) {
			RootBeanDefinition validatorDef = new RootBeanDefinition(LocalValidatorFactoryBean.class);
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

	private RuntimeBeanReference getMessageCodesResolver(Element element, Object source, ParserContext parserContext) {
		if (element.hasAttribute("message-codes-resolver")) {
			return new RuntimeBeanReference(element.getAttribute("message-codes-resolver"));
		} else {
			return null;
		}
	}

	private ManagedList<?> getArgumentResolvers(Element element, Object source, ParserContext parserContext) {
		Element resolversElement = DomUtils.getChildElementByTagName(element, "argument-resolvers");
		if (resolversElement != null) {
			ManagedList<BeanDefinitionHolder> argumentResolvers = extractBeanSubElements(resolversElement, parserContext);
			return wrapWebArgumentResolverBeanDefs(argumentResolvers);
		}
		return null;
	}

	private ManagedList<?> getReturnValueHandlers(Element element, Object source, ParserContext parserContext) {
		Element handlersElement = DomUtils.getChildElementByTagName(element, "return-value-handlers");
		if (handlersElement != null) {
			return extractBeanSubElements(handlersElement, parserContext);
		}
		return null;
	}

	private ManagedList<?> getMessageConverters(Element element, Object source, ParserContext parserContext) {
		Element convertersElement = DomUtils.getChildElementByTagName(element, "message-converters");
		ManagedList<? super Object> messageConverters = new ManagedList<Object>();
		if (convertersElement != null) {
			messageConverters.setSource(source);
			for (Element converter : DomUtils.getChildElementsByTagName(convertersElement, "bean")) {
				BeanDefinitionHolder beanDef = parserContext.getDelegate().parseBeanDefinitionElement(converter);
				beanDef = parserContext.getDelegate().decorateBeanDefinitionIfRequired(converter, beanDef);
				messageConverters.add(beanDef);
			}
		}

		if (convertersElement == null || Boolean.valueOf(convertersElement.getAttribute("register-defaults"))) {
			messageConverters.setSource(source);
			messageConverters.add(createConverterBeanDefinition(ByteArrayHttpMessageConverter.class, source));

			RootBeanDefinition stringConverterDef = createConverterBeanDefinition(StringHttpMessageConverter.class,
					source);
			stringConverterDef.getPropertyValues().add("writeAcceptCharset", false);
			messageConverters.add(stringConverterDef);

			messageConverters.add(createConverterBeanDefinition(ResourceHttpMessageConverter.class, source));
			messageConverters.add(createConverterBeanDefinition(SourceHttpMessageConverter.class, source));
			messageConverters.add(createConverterBeanDefinition(XmlAwareFormHttpMessageConverter.class, source));
			if (jaxb2Present) {
				messageConverters
						.add(createConverterBeanDefinition(Jaxb2RootElementHttpMessageConverter.class, source));
			}
			if (jacksonPresent) {
				messageConverters.add(createConverterBeanDefinition(MappingJacksonHttpMessageConverter.class, source));
			}
			if (romePresent) {
				messageConverters.add(createConverterBeanDefinition(AtomFeedHttpMessageConverter.class, source));
				messageConverters.add(createConverterBeanDefinition(RssChannelHttpMessageConverter.class, source));
			}
		}
		return messageConverters;
	}

	private RootBeanDefinition createConverterBeanDefinition(Class<? extends HttpMessageConverter> converterClass,
			Object source) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(converterClass);
		beanDefinition.setSource(source);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		return beanDefinition;
	}

	private ManagedList<BeanDefinitionHolder> extractBeanSubElements(Element parentElement, ParserContext parserContext) {
		ManagedList<BeanDefinitionHolder> list = new ManagedList<BeanDefinitionHolder>();
		list.setSource(parserContext.extractSource(parentElement));
		for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, "bean")) {
			BeanDefinitionHolder beanDef = parserContext.getDelegate().parseBeanDefinitionElement(beanElement);
			beanDef = parserContext.getDelegate().decorateBeanDefinitionIfRequired(beanElement, beanDef);
			list.add(beanDef);
		}
		return list;
	}

	private ManagedList<BeanDefinitionHolder> wrapWebArgumentResolverBeanDefs(List<BeanDefinitionHolder> beanDefs) {
		ManagedList<BeanDefinitionHolder> result = new ManagedList<BeanDefinitionHolder>();

		for (BeanDefinitionHolder beanDef : beanDefs) {
			String className = beanDef.getBeanDefinition().getBeanClassName();
			Class<?> clazz = ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader());

			if (WebArgumentResolver.class.isAssignableFrom(clazz)) {
				RootBeanDefinition adapter = new RootBeanDefinition(ServletWebArgumentResolverAdapter.class);
				adapter.getConstructorArgumentValues().addIndexedArgumentValue(0, beanDef);
				result.add(new BeanDefinitionHolder(adapter, beanDef.getBeanName() + "Adapter"));
			} else {
				result.add(beanDef);
			}
		}

		return result;
	}
}
