/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures that @Configuration is supported properly as a meta-annotation.
 *
 * @author Chris Beams
 */
class ConfigurationMetaAnnotationTests {

	@Test
	void customConfigurationStereotype() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		assertThat(ctx.containsBean("customName")).isTrue();
		TestBean a = ctx.getBean("a", TestBean.class);
		TestBean b = ctx.getBean("b", TestBean.class);
		assertThat(b).isSameAs(a.getSpouse());
		ctx.close();
	}


	@TestConfiguration("customName")
	static class Config {
		@Bean
		TestBean a() {
			TestBean a = new TestBean();
			a.setSpouse(b());
			return a;
		}

		@Bean
		TestBean b() {
			return new TestBean();
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Configuration
	@interface TestConfiguration {

		@AliasFor(annotation = Configuration.class)
		String value() default "";
	}

}
