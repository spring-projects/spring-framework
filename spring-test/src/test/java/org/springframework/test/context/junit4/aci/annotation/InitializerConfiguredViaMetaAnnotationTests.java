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

package org.springframework.test.context.junit4.aci.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.aci.annotation.InitializerConfiguredViaMetaAnnotationTests.ComposedContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.assertEquals;

/**
 * Integration test that demonstrates how to register one or more {@code @Configuration}
 * classes via an {@link ApplicationContextInitializer} in a composed annotation so
 * that certain {@code @Configuration} classes are always registered whenever the composed
 * annotation is used, even if the composed annotation is used to declare additional
 * {@code @Configuration} classes.
 *
 * <p>This class has been implemented in response to the following Stack Overflow question:
 * <a href="http://stackoverflow.com/questions/35733344/can-contextconfiguration-in-a-custom-annotation-be-merged">
 * Can {@code @ContextConfiguration} in a custom annotation be merged?</a>
 *
 * @author Sam Brannen
 * @since 4.3
 */
@RunWith(SpringRunner.class)
@ComposedContextConfiguration(BarConfig.class)
public class InitializerConfiguredViaMetaAnnotationTests {

	@Autowired
	String foo;

	@Autowired
	String bar;

	@Autowired
	List<String> strings;


	@Test
	public void beansFromInitializerAndComposedAnnotation() {
		assertEquals(2, strings.size());
		assertEquals("foo", foo);
		assertEquals("bar", bar);
	}


	static class FooConfigInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			new AnnotatedBeanDefinitionReader(applicationContext).register(FooConfig.class);
		}
	}

	@ContextConfiguration(loader = AnnotationConfigContextLoader.class, initializers = FooConfigInitializer.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface ComposedContextConfiguration {

		@AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
		Class<?>[] value() default {};
	}

}
