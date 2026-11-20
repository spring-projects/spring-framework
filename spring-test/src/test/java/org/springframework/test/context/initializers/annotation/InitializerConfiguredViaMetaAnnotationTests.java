/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.initializers.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.initializers.annotation.InitializerConfiguredViaMetaAnnotationTests.ComposedContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that demonstrates how to register one or more {@code @Configuration}
 * classes via an {@link ApplicationContextInitializer} in a composed annotation so
 * that certain {@code @Configuration} classes are always registered whenever the composed
 * annotation is used, even if the composed annotation is used to declare additional
 * {@code @Configuration} classes.
 *
 * <p>This class has been implemented in response to the following Stack Overflow question:
 * <a href="https://stackoverflow.com/questions/35733344/can-contextconfiguration-in-a-custom-annotation-be-merged">
 * Can {@code @ContextConfiguration} in a custom annotation be merged?</a>
 *
 * @author Sam Brannen
 * @since 4.3
 */
@ExtendWith(SpringExtension.class)
@ComposedContextConfiguration(BarConfig.class)
class InitializerConfiguredViaMetaAnnotationTests {

	@Autowired
	String foo;

	@Autowired
	String bar;

	@Autowired
	List<String> strings;


	@Test
	void beansFromInitializerAndComposedAnnotation() {
		assertThat(strings).hasSize(2);
		assertThat(foo).isEqualTo("foo");
		assertThat(bar).isEqualTo("bar");
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
