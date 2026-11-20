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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yanming Zhou
 */
class AnnotatedBeanDefinitionReaderTests {

	@Test
	@SuppressWarnings("unchecked")
	void registerBeanWithQualifiers() {
		GenericApplicationContext context = new GenericApplicationContext();
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);

		reader.registerBean(TestBean.class, "primary", Primary.class);
		assertThat(context.getBeanDefinition("primary").isPrimary()).isTrue();

		reader.registerBean(TestBean.class, "fallback", Fallback.class);
		assertThat(context.getBeanDefinition("fallback").isFallback()).isTrue();

		reader.registerBean(TestBean.class, "lazy", Lazy.class);
		assertThat(context.getBeanDefinition("lazy").isLazyInit()).isTrue();

		reader.registerBean(TestBean.class, "customQualifier", CustomQualifier.class);
		assertThat(context.getBeanDefinition("customQualifier"))
				.isInstanceOfSatisfying(AbstractBeanDefinition.class, abd ->
					assertThat(abd.hasQualifier(CustomQualifier.class.getTypeName())).isTrue());
	}

	@Lazy(false)
	static class TestBean {
	}

	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	@interface CustomQualifier {
	}

}
