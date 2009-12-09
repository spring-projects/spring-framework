package org.springframework.context.support;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

import org.junit.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;

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
			public Foo convert(String source) {
				return new Foo();
			}
		});
		converters.add(new ConverterFactory<String, Bar>() {
			public <T extends Bar> Converter<String, T> getConverter(Class<T> targetType) {
				return new Converter<String, T> () {
					public T convert(String source) {
						return (T) new Bar();
					}
				};
			}
		});
		converters.add(new GenericConverter() {
			public Set<ConvertiblePair> getConvertibleTypes() {
				return Collections.singleton(new ConvertiblePair(String.class, Baz.class));
			}
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

	public static class Foo {
		
	}
	
	public static class Bar {
		
	}
	
	public static class Baz {
		
	}
}
