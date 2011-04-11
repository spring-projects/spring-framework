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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Feature;
import org.springframework.context.annotation.FeatureConfiguration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMethodAdapter;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletWebArgumentResolverAdapter;

/**
 * Integration tests for the {@link MvcAnnotationDriven} feature specification.
 * @author Rossen Stoyanchev
 * @author Chris Beams
 * @since 3.1
 */
public class MvcAnnotationDrivenFeatureTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testMessageCodesResolver() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MvcFeature.class, MvcBeans.class);
		ctx.refresh();
		RequestMappingHandlerMethodAdapter adapter = ctx.getBean(RequestMappingHandlerMethodAdapter.class);
		assertNotNull(adapter);
		Object initializer = new DirectFieldAccessor(adapter).getPropertyValue("webBindingInitializer");
		assertNotNull(initializer);
		MessageCodesResolver resolver = ((ConfigurableWebBindingInitializer) initializer).getMessageCodesResolver();
		assertNotNull(resolver);
		assertEquals("test.foo.bar", resolver.resolveMessageCodes("foo", "bar")[0]);
		Object value = new DirectFieldAccessor(adapter).getPropertyValue("customArgumentResolvers");
		assertNotNull(value);
		List<HandlerMethodArgumentResolver> resolvers = (List<HandlerMethodArgumentResolver>) value;
		assertEquals(2, resolvers.size());
		assertTrue(resolvers.get(0) instanceof ServletWebArgumentResolverAdapter);
		assertTrue(resolvers.get(1) instanceof TestHandlerMethodArgumentResolver);
		Object converters = new DirectFieldAccessor(adapter).getPropertyValue("messageConverters");
		assertNotNull(converters);
		List<HttpMessageConverter<?>> convertersArray = (List<HttpMessageConverter<?>>) converters;
		assertTrue("Default converters are registered in addition to the custom one", convertersArray.size() > 1);
		assertTrue(convertersArray.get(0) instanceof StringHttpMessageConverter);
	}

}

@FeatureConfiguration
class MvcFeature {
	@Feature
	public MvcAnnotationDriven annotationDriven(MvcBeans mvcBeans) {
		return new MvcAnnotationDriven()
			.conversionService(mvcBeans.conversionService())
			.messageCodesResolver(mvcBeans.messageCodesResolver())
			.validator(mvcBeans.validator())
			.messageConverters(new StringHttpMessageConverter())
			.argumentResolvers(new TestWebArgumentResolver())
			.argumentResolvers(new TestHandlerMethodArgumentResolver());
	}
}

@Configuration
class MvcBeans {
	@Bean
	public FormattingConversionService conversionService() {
		return new DefaultFormattingConversionService();
	}
	@Bean
	public Validator validator() {
		return new LocalValidatorFactoryBean();
	}
	@Bean MessageCodesResolver messageCodesResolver() {
		return new TestMessageCodesResolver();
	}
}

