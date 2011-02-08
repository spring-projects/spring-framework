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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentRegistrar;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.config.AbstractSpecificationExecutor;
import org.springframework.context.config.ExecutorContext;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * Executes {@link MvcAnnotationDriven} specifications, creating and registering
 * bean definitions as appropriate based on the configuration within.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see MvcAnnotationDriven
 */
final class MvcAnnotationDrivenExecutor extends AbstractSpecificationExecutor<MvcAnnotationDriven> {

	private static final boolean jsr303Present = ClassUtils.isPresent("javax.validation.Validator",
			AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder",
			AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jacksonPresent = ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper",
			AnnotationDrivenBeanDefinitionParser.class.getClassLoader())
			&& ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator",
					AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static boolean romePresent = ClassUtils.isPresent("com.sun.syndication.feed.WireFeed",
			AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	@Override
	public void doExecute(MvcAnnotationDriven spec, ExecutorContext executorContext) {
		ComponentRegistrar registrar = executorContext.getRegistrar();
		Object source = spec.source();

		RootBeanDefinition annMappingDef = new RootBeanDefinition(DefaultAnnotationHandlerMapping.class);
		annMappingDef.setSource(source);
		annMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		annMappingDef.getPropertyValues().add("order", 0);
		String annMappingName = registrar.registerWithGeneratedName(annMappingDef);

		Object conversionService = getConversionService(spec, registrar);
		Object validator = getValidator(spec, registrar);
		Object messageCodesResolver = getMessageCodesResolver(spec, registrar);

		RootBeanDefinition bindingDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
		bindingDef.setSource(source);
		bindingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		bindingDef.getPropertyValues().add("conversionService", conversionService);
		bindingDef.getPropertyValues().add("validator", validator);
		bindingDef.getPropertyValues().add("messageCodesResolver", messageCodesResolver);

		ManagedList<? super Object> messageConverters = getMessageConverters(spec, registrar);

		RootBeanDefinition annAdapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
		annAdapterDef.setSource(source);
		annAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		annAdapterDef.getPropertyValues().add("webBindingInitializer", bindingDef);
		annAdapterDef.getPropertyValues().add("messageConverters", messageConverters);
		if (!spec.argumentResolvers().isEmpty()) {
			annAdapterDef.getPropertyValues().add("customArgumentResolvers", spec.argumentResolvers());
		}
		String annAdapterName = registrar.registerWithGeneratedName(annAdapterDef);

		RootBeanDefinition csInterceptorDef = new RootBeanDefinition(ConversionServiceExposingInterceptor.class);
		csInterceptorDef.setSource(source);
		csInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, conversionService);
		RootBeanDefinition mappedCsInterceptorDef = new RootBeanDefinition(MappedInterceptor.class);
		mappedCsInterceptorDef.setSource(source);
		mappedCsInterceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		mappedCsInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, (Object) null);
		mappedCsInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(1, csInterceptorDef);
		String mappedInterceptorName = registrar.registerWithGeneratedName(mappedCsInterceptorDef);

		RootBeanDefinition annExceptionResolver = new RootBeanDefinition(AnnotationMethodHandlerExceptionResolver.class);
		annExceptionResolver.setSource(source);
		annExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		annExceptionResolver.getPropertyValues().add("messageConverters", messageConverters);
		annExceptionResolver.getPropertyValues().add("order", 0);
		String annExceptionResolverName = registrar.registerWithGeneratedName(annExceptionResolver);

		RootBeanDefinition responseStatusExceptionResolver = new RootBeanDefinition(
				ResponseStatusExceptionResolver.class);
		responseStatusExceptionResolver.setSource(source);
		responseStatusExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		responseStatusExceptionResolver.getPropertyValues().add("order", 1);
		String responseStatusExceptionResolverName = registrar
				.registerWithGeneratedName(responseStatusExceptionResolver);

		RootBeanDefinition defaultExceptionResolver = new RootBeanDefinition(DefaultHandlerExceptionResolver.class);
		defaultExceptionResolver.setSource(source);
		defaultExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		defaultExceptionResolver.getPropertyValues().add("order", 2);
		String defaultExceptionResolverName = registrar.registerWithGeneratedName(defaultExceptionResolver);

		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(spec.sourceName(), source);
		compDefinition.addNestedComponent(new BeanComponentDefinition(annMappingDef, annMappingName));
		compDefinition.addNestedComponent(new BeanComponentDefinition(annAdapterDef, annAdapterName));
		compDefinition.addNestedComponent(new BeanComponentDefinition(annExceptionResolver, annExceptionResolverName));
		compDefinition.addNestedComponent(new BeanComponentDefinition(responseStatusExceptionResolver,
				responseStatusExceptionResolverName));
		compDefinition.addNestedComponent(new BeanComponentDefinition(defaultExceptionResolver,
				defaultExceptionResolverName));
		compDefinition.addNestedComponent(new BeanComponentDefinition(mappedCsInterceptorDef, mappedInterceptorName));
		registrar.registerComponent(compDefinition);
	}

	private Object getConversionService(MvcAnnotationDriven spec, ComponentRegistrar registrar) {
		if (spec.conversionService() != null) {
			return getBeanOrReference(spec.conversionService());
		} else {
			RootBeanDefinition conversionDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
			conversionDef.setSource(spec.source());
			conversionDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String conversionName = registrar.registerWithGeneratedName(conversionDef);
			registrar.registerComponent(new BeanComponentDefinition(conversionDef, conversionName));
			return new RuntimeBeanReference(conversionName);
		}
	}

	private Object getValidator(MvcAnnotationDriven spec, ComponentRegistrar registrar) {
		if (spec.validator() != null) {
			return getBeanOrReference(spec.validator());
		} else if (jsr303Present) {
			RootBeanDefinition validatorDef = new RootBeanDefinition(LocalValidatorFactoryBean.class);
			validatorDef.setSource(spec.source());
			validatorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String validatorName = registrar.registerWithGeneratedName(validatorDef);
			registrar.registerComponent(new BeanComponentDefinition(validatorDef, validatorName));
			return new RuntimeBeanReference(validatorName);
		} else {
			return null;
		}
	}

	private Object getMessageCodesResolver(MvcAnnotationDriven spec, ComponentRegistrar registrar) {
		if (spec.messageCodesResolver() != null) {
			return getBeanOrReference(spec.messageCodesResolver());
		} else {
			return null;
		}
	}

	private ManagedList<? super Object> getMessageConverters(MvcAnnotationDriven spec, ComponentRegistrar registrar) {
		ManagedList<? super Object> messageConverters = new ManagedList<Object>();
		Object source = spec.source();
		messageConverters.setSource(source);
		messageConverters.addAll(spec.messageConverters());
		if (spec.shouldRegisterDefaultMessageConverters()) {
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

	@SuppressWarnings("rawtypes")
	private RootBeanDefinition createConverterBeanDefinition(Class<? extends HttpMessageConverter> converterClass,
			Object source) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(converterClass);
		beanDefinition.setSource(source);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		return beanDefinition;
	}

	private Object getBeanOrReference(Object bean) {
		if (bean != null && bean instanceof String) {
			return new RuntimeBeanReference((String) bean);
		} else {
			return bean;
		}
	}

}
