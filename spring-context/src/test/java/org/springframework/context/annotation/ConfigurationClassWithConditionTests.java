/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Conditional} beans.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
@SuppressWarnings("resource")
class ConfigurationClassWithConditionTests {

	@Test
	void conditionalOnMissingBeanMatch() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(BeanOneConfiguration.class, BeanTwoConfiguration.class);
		ctx.refresh();
		assertThat(ctx.containsBean("bean1")).isTrue();
		assertThat(ctx.containsBean("bean2")).isFalse();
		assertThat(ctx.containsBean("configurationClassWithConditionTests.BeanTwoConfiguration")).isFalse();
	}

	@Test
	void conditionalOnMissingBeanNoMatch() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(BeanTwoConfiguration.class);
		ctx.refresh();
		assertThat(ctx.containsBean("bean1")).isFalse();
		assertThat(ctx.containsBean("bean2")).isTrue();
		assertThat(ctx.containsBean("configurationClassWithConditionTests.BeanTwoConfiguration")).isTrue();
	}

	@Test
	void conditionalOnBeanMatch() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(BeanOneConfiguration.class, BeanThreeConfiguration.class);
		ctx.refresh();
		assertThat(ctx.containsBean("bean1")).isTrue();
		assertThat(ctx.containsBean("bean3")).isTrue();
	}

	@Test
	void conditionalOnBeanNoMatch() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(BeanThreeConfiguration.class);
		ctx.refresh();
		assertThat(ctx.containsBean("bean1")).isFalse();
		assertThat(ctx.containsBean("bean3")).isFalse();
	}

	@Test
	void metaConditional() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigurationWithMetaCondition.class);
		ctx.refresh();
		assertThat(ctx.containsBean("bean")).isTrue();
	}

	@Test
	void metaConditionalWithAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(ConfigurationWithMetaCondition.class.getName()));
		ctx.refresh();
		assertThat(ctx.containsBean("bean")).isTrue();
	}

	@Test
	void nonConfigurationClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(NonConfigurationClass.class);
		ctx.refresh();
		assertThat(ctx.containsBean("bean1")).isFalse();
	}

	@Test
	void nonConfigurationClassWithAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(NonConfigurationClass.class.getName()));
		ctx.refresh();
		assertThat(ctx.containsBean("bean1")).isFalse();
	}

	@Test
	void methodConditional() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConditionOnMethodConfiguration.class);
		ctx.refresh();
		assertThat(ctx.containsBean("bean1")).isFalse();
	}

	@Test
	void methodConditionalWithAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(ConditionOnMethodConfiguration.class.getName()));
		ctx.refresh();
		assertThat(ctx.containsBean("bean1")).isFalse();
	}

	@Test
	void importsNotCreated() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportsNotCreated.class);
		ctx.refresh();
	}

	@Test
	void conditionOnOverriddenMethodHonored() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithBeanSkipped.class);
		assertThat(context.getBeansOfType(ExampleBean.class)).isEmpty();
	}

	@Test
	void noConditionOnOverriddenMethodHonored() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithBeanReactivated.class);
		Map<String, ExampleBean> beans = context.getBeansOfType(ExampleBean.class);
		assertThat(beans).containsOnlyKeys("baz");
	}

	@Test
	void configWithAlternativeBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithAlternativeBeans.class);
		Map<String, ExampleBean> beans = context.getBeansOfType(ExampleBean.class);
		assertThat(beans).containsOnlyKeys("baz");
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
	public @interface MetaConditional {

		String value();
	}

	@Conditional(NeverCondition.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.METHOD})
	public @interface Never {
	}

	@Conditional(AlwaysCondition.class)
	@Never
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.METHOD})
	public @interface MetaNever {
	}

	static class NoBeanOneCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return !context.getBeanFactory().containsBeanDefinition("bean1");
		}
	}

	static class HasBeanOneCondition implements ConfigurationCondition {

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return context.getBeanFactory().containsBeanDefinition("bean1");
		}
	}

	static class MetaConditionalFilter implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(MetaConditional.class.getName()));
			assertThat(attributes.getString("value")).isEqualTo("test");
			return true;
		}
	}

	static class NeverCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}
	}

	static class AlwaysCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return true;
		}
	}

	@Component
	@MetaNever
	static class NonConfigurationClass {

		@Bean
		public ExampleBean bean1() {
			return new ExampleBean();
		}
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
	@Import({ConfigurationNotCreated.class, RegistrarNotCreated.class, ImportSelectorNotCreated.class})
	static class ImportsNotCreated {

		static {
			if (true) {
				throw new RuntimeException();
			}
		}
	}

	@Configuration
	static class ConfigurationNotCreated {

		static {
			if (true) {
				throw new RuntimeException();
			}
		}
	}

	static class RegistrarNotCreated implements ImportBeanDefinitionRegistrar {

		static {
			if (true) {
				throw new RuntimeException();
			}
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
		}
	}

	static class ImportSelectorNotCreated implements ImportSelector {

		static {
			if (true) {
				throw new RuntimeException();
			}
		}

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] {};
		}

	}

	static class ExampleBean {
	}

	@Configuration
	static class ConfigWithBeanActive {

		@Bean
		public ExampleBean baz() {
			return new ExampleBean();
		}
	}

	static class ConfigWithBeanSkipped extends ConfigWithBeanActive {

		@Override
		@Bean
		@Conditional(NeverCondition.class)
		public ExampleBean baz() {
			return new ExampleBean();
		}
	}

	static class ConfigWithBeanReactivated extends ConfigWithBeanSkipped {

		@Override
		@Bean
		public ExampleBean baz() {
			return new ExampleBean();
		}
	}

	@Configuration
	static class ConfigWithAlternativeBeans {

		@Bean(name = "baz")
		@Conditional(AlwaysCondition.class)
		public ExampleBean baz1() {
			return new ExampleBean();
		}

		@Bean(name = "baz")
		@Conditional(NeverCondition.class)
		public ExampleBean baz2() {
			return new ExampleBean();
		}
	}

}
