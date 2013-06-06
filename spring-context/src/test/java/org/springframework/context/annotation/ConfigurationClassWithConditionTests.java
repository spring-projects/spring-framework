/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Test for {@link Conditional} beans.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class ConfigurationClassWithConditionTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void conditionalOnMissingBeanMatch() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(BeanOneConfiguration.class, BeanTwoConfiguration.class);
		ctx.refresh();
		assertTrue(ctx.containsBean("bean1"));
		assertFalse(ctx.containsBean("bean2"));
		assertFalse(ctx.containsBean("configurationClassWithConditionTests.BeanTwoConfiguration"));
	}

	@Test
	public void conditionalOnMissingBeanNoMatch() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(BeanTwoConfiguration.class);
		ctx.refresh();
		assertFalse(ctx.containsBean("bean1"));
		assertTrue(ctx.containsBean("bean2"));
		assertTrue(ctx.containsBean("configurationClassWithConditionTests.BeanTwoConfiguration"));
	}

	@Test
	public void conditionalOnBeanMatch() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(BeanOneConfiguration.class, BeanThreeConfiguration.class);
		ctx.refresh();
		assertTrue(ctx.containsBean("bean1"));
		assertTrue(ctx.containsBean("bean3"));
	}

	@Test
	public void conditionalOnBeanNoMatch() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(BeanThreeConfiguration.class);
		ctx.refresh();
		assertFalse(ctx.containsBean("bean1"));
		assertFalse(ctx.containsBean("bean3"));
	}

	@Test
	public void metaConditional() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigurationWithMetaCondition.class);
		ctx.refresh();
		assertTrue(ctx.containsBean("bean"));
	}

	@Test
	public void nonConfigurationClass() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(NonConfigurationClass.class);
		ctx.refresh();
		thrown.expect(NoSuchBeanDefinitionException.class);
		assertNull(ctx.getBean(NonConfigurationClass.class));
	}

	@Test
	public void methodConditional() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConditionOnMethodConfiguration.class);
		ctx.refresh();
		thrown.expect(NoSuchBeanDefinitionException.class);
		assertNull(ctx.getBean(ExampleBean.class));
	}

	@Test
	public void importsNotCreated() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportsNotCreated.class);
		ctx.refresh();
	}

	@Configuration
	static class BeanOneConfiguration {
		@Bean
		public ExampleBean bean1() {
			return new ExampleBean();
		}
	}

	@Configuration
	@Conditional(NoBeanOneCondition.class)
	static class BeanTwoConfiguration {
		@Bean
		public ExampleBean bean2() {
			return new ExampleBean();
		}
	}

	@Configuration
	@Conditional(HasBeanOneCondition.class)
	static class BeanThreeConfiguration {
		@Bean
		public ExampleBean bean3() {
			return new ExampleBean();
		}
	}

	@Configuration
	@MetaConditional("test")
	static class ConfigurationWithMetaCondition {
		@Bean
		public ExampleBean bean() {
			return new ExampleBean();
		}
	}

	@Conditional(MetaConditionalFilter.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface MetaConditional {
		String value();
	}

	@Conditional(NeverCondition.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.METHOD})
	public static @interface Never {
	}

	static class NoBeanOneCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return !context.getBeanFactory().containsBeanDefinition("bean1");
		}
	}

	static class HasBeanOneCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return context.getBeanFactory().containsBeanDefinition("bean1");
		}
	}

	static class MetaConditionalFilter implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(MetaConditional.class.getName()));
			assertThat(attributes.getString("value"), equalTo("test"));
			return true;
		}
	}

	static class NeverCondition implements Condition {
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}
	}

	@Component
	@Never
	static class NonConfigurationClass {
	}

	@Configuration
	static class ConditionOnMethodConfiguration {

		@Bean
		@Never
		public ExampleBean bean1() {
			return new ExampleBean();
		}
	}

	@Configuration
	@Never
	@Import({ ConfigurationNotCreated.class, RegistrarNotCreated.class, ImportSelectorNotCreated.class })
	static class ImportsNotCreated {
		static {
			if (true) throw new RuntimeException();
		}
	}

	@Configuration
	static class ConfigurationNotCreated {
		static {
			if (true) throw new RuntimeException();
		}
	}

	static class RegistrarNotCreated implements ImportBeanDefinitionRegistrar {
		static {
			if (true) throw new RuntimeException();
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
		}
	}

	static class ImportSelectorNotCreated implements ImportSelector {

		static {
			if (true) throw new RuntimeException();
		}

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] {};
		}

	}

	static class ExampleBean {
	}

}
