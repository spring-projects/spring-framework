/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Wilkinson
 */
class FactoryMethodResolutionTests {

	@Test
	void factoryMethodCanBeResolvedWithBeanMetadataCachingEnabled() {
		assertThatFactoryMethodCanBeResolved(true);
	}

	@Test
	void factoryMethodCanBeResolvedWithBeanMetadataCachingDisabled() {
		assertThatFactoryMethodCanBeResolved(false);
	}

	private void assertThatFactoryMethodCanBeResolved(boolean cache) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.getBeanFactory().setCacheBeanMetadata(cache);
			context.register(ImportSelectorConfiguration.class);
			context.refresh();
			BeanDefinition definition = context.getBeanFactory().getMergedBeanDefinition("exampleBean");
			assertThat(((RootBeanDefinition)definition).getResolvedFactoryMethod()).isNotNull();
		}
	}


	@Configuration
	@Import(ExampleImportSelector.class)
	static class ImportSelectorConfiguration {
	}


	static class ExampleImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] { TestConfiguration.class.getName() };
		}
	}


	@Configuration
	static class TestConfiguration {

		@Bean
		@ExampleAnnotation
		public ExampleBean exampleBean() {
			return new ExampleBean();
		}
	}


	static class ExampleBean {
	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleAnnotation {
	}

}
