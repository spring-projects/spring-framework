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

package org.springframework.context.support;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.beans.ResourceTestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import static org.junit.Assert.*;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class ConversionServiceFactoryBeanTests {

	@Test
	public void createDefaultConversionService() {
		ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
		factory.afterPropertiesSet();
		ConversionService service = factory.getObject();
		assertTrue(service.canConvert(String.class, Integer.class));
	}

	@Test
	public void createDefaultConversionServiceWithSupplements() {
		ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
		Set<Object> converters = new HashSet<Object>();
		converters.add(new Converter<String, Foo>() {
			@Override
			public Foo convert(String source) {
				return new Foo();
			}
		});
		converters.add(new ConverterFactory<String, Bar>() {
			@Override
			public <T extends Bar> Converter<String, T> getConverter(Class<T> targetType) {
				return new Converter<String, T> () {
					@SuppressWarnings("unchecked")
					@Override
					public T convert(String source) {
						return (T) new Bar();
					}
				};
			}
		});
		converters.add(new GenericConverter() {
			@Override
			public Set<ConvertiblePair> getConvertibleTypes() {
				return Collections.singleton(new ConvertiblePair(String.class, Baz.class));
			}
			@Override
			public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
				return new Baz();
			}
		});
		factory.setConverters(converters);
		factory.afterPropertiesSet();
		ConversionService service = factory.getObject();
		assertTrue(service.canConvert(String.class, Integer.class));
		assertTrue(service.canConvert(String.class, Foo.class));
		assertTrue(service.canConvert(String.class, Bar.class));
		assertTrue(service.canConvert(String.class, Baz.class));
	}

	@Test(expected=IllegalArgumentException.class)
	public void createDefaultConversionServiceWithInvalidSupplements() {
		ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
		Set<Object> converters = new HashSet<Object>();
		converters.add("bogus");
		factory.setConverters(converters);
		factory.afterPropertiesSet();
	}

	@Test
	public void conversionServiceInApplicationContext() {
		doTestConversionServiceInApplicationContext("conversionService.xml", ClassPathResource.class);
	}

	@Test
	public void conversionServiceInApplicationContextWithResourceOverriding() {
		doTestConversionServiceInApplicationContext("conversionServiceWithResourceOverriding.xml", FileSystemResource.class);
	}

	private void doTestConversionServiceInApplicationContext(String fileName, Class<?> resourceClass) {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(fileName, getClass());
		ResourceTestBean tb = ctx.getBean("resourceTestBean", ResourceTestBean.class);
		assertTrue(resourceClass.isInstance(tb.getResource()));
		assertTrue(tb.getResourceArray().length > 0);
		assertTrue(resourceClass.isInstance(tb.getResourceArray()[0]));
		assertTrue(tb.getResourceMap().size() == 1);
		assertTrue(resourceClass.isInstance(tb.getResourceMap().get("key1")));
		assertTrue(tb.getResourceArrayMap().size() == 1);
		assertTrue(tb.getResourceArrayMap().get("key1").length > 0);
		assertTrue(resourceClass.isInstance(tb.getResourceArrayMap().get("key1")[0]));
	}


	public static class Foo {
	}

	public static class Bar {
	}

	public static class Baz {
	}

	public static class ComplexConstructorArgument {

		public ComplexConstructorArgument(Map<String, Class<?>> map) {
			assertTrue(!map.isEmpty());
			assertTrue(map.keySet().iterator().next() instanceof String);
			assertTrue(map.values().iterator().next() instanceof Class);
		}
	}

}
