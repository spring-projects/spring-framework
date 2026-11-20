/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.convention;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.bean.override.example.CustomQualifier;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestBean} that use by-type lookup.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
public class TestBeanByTypeLookupIntegrationTests {

	@TestBean
	MessageService messageService;

	@TestBean
	ExampleService anyNameForService;

	@TestBean(methodName = "someString")
	@Qualifier("prefer")
	StringBuilder anyNameForStringBuilder;

	@TestBean(methodName = "someString2")
	@CustomQualifier
	StringBuilder anyNameForStringBuilder2;

	@TestBean
	Number prototypeNumber;


	static MessageService messageService() {
		return () -> "mocked nonexistent bean definition";
	}

	static ExampleService anyNameForService() {
		return new RealExampleService("Mocked greeting");
	}

	static StringBuilder someString() {
		return new StringBuilder("Prefer TestBean String");
	}

	static StringBuilder someString2() {
		return new StringBuilder("CustomQualifier TestBean String");
	}

	static Number prototypeNumber() {
		return 42;
	}

	@Test
	void overrideIsFoundByTypeForNonexistentBeanDefinition(ApplicationContext ctx) {
		assertThat(this.messageService).isSameAs(ctx.getBean(MessageService.class));
		assertThat(this.messageService.getMessage()).isEqualTo("mocked nonexistent bean definition");
	}

	@Test
	void overrideIsFoundByType(ApplicationContext ctx) {
		assertThat(this.anyNameForService)
				.isSameAs(ctx.getBean("example"))
				.isSameAs(ctx.getBean(ExampleService.class));

		assertThat(this.anyNameForService.greeting()).isEqualTo("Mocked greeting");
	}

	@Test
	void overrideIsFoundByTypeWithQualifierDisambiguation(ApplicationContext ctx) {
		assertThat(this.anyNameForStringBuilder)
				.as("direct qualifier")
				.isSameAs(ctx.getBean("two"))
				.hasToString("Prefer TestBean String");

		assertThat(this.anyNameForStringBuilder2)
				.as("meta qualifier")
				.isSameAs(ctx.getBean("three"))
				.hasToString("CustomQualifier TestBean String");

		assertThat(ctx.getBean("one")).as("no qualifier needed").hasToString("Prod One");
	}

	@Test
	void overrideIsFoundByTypeForPrototypeBeanDefinition(ConfigurableApplicationContext ctx) {
		assertThat(ctx.getBeanFactory().getBeanDefinition("prototypeNumber").isSingleton()).as("isSingleton").isTrue();
		assertThat(this.prototypeNumber).isSameAs(ctx.getBean(Number.class));
		assertThat(this.prototypeNumber).isEqualTo(42);
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean("example")
		ExampleService bean1() {
			return new RealExampleService("Production hello");
		}

		@Bean("one")
		StringBuilder beanString1() {
			return new StringBuilder("Prod One");
		}

		@Bean("two")
		@Qualifier("prefer")
		StringBuilder beanString2() {
			return new StringBuilder("Prod Two");
		}

		@Bean("three")
		@CustomQualifier
		StringBuilder beanString3() {
			return new StringBuilder("Prod Three");
		}

		@Bean
		@Scope("prototype")
		Number prototypeNumber() {
			return -999;
		}
	}

	@FunctionalInterface
	public interface MessageService {

		String getMessage();
	}

}
