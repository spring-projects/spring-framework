/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests proving that the various attributes available via the {@link Bean}
 * annotation are correctly reflected in the {@link BeanDefinition} created when
 * processing the {@link Configuration} class.
 *
 * <p>Also includes tests proving that using {@link Lazy} and {@link Primary}
 * annotations in conjunction with Bean propagate their respective metadata
 * correctly into the resulting BeanDefinition
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class BeanAnnotationAttributePropagationTests {

	@Test
	void autowireCandidateMetadataIsPropagated() {
		@Configuration class Config {
			@Bean(autowireCandidate=false) Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).isAutowireCandidate()).as("autowire candidate flag was not propagated").isFalse();
	}

	@Test
	void initMethodMetadataIsPropagated() {
		@Configuration class Config {
			@Bean(initMethod="start") Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).getInitMethodName()).as("init method name was not propagated").isEqualTo("start");
	}

	@Test
	void destroyMethodMetadataIsPropagated() {
		@Configuration class Config {
			@Bean(destroyMethod="destroy") Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).getDestroyMethodName()).as("destroy method name was not propagated").isEqualTo("destroy");
	}

	@Test
	void dependsOnMetadataIsPropagated() {
		@Configuration class Config {
			@Bean() @DependsOn({"bar", "baz"}) Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).getDependsOn()).as("dependsOn metadata was not propagated").isEqualTo(new String[] {"bar", "baz"});
	}

	@Test
	void primaryMetadataIsPropagated() {
		@Configuration class Config {
			@Primary @Bean
			Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).isPrimary()).as("primary metadata was not propagated").isTrue();
	}

	@Test
	void primaryMetadataIsFalseByDefault() {
		@Configuration class Config {
			@Bean Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).isPrimary()).as("@Bean methods should be non-primary by default").isFalse();
	}

	@Test
	void lazyMetadataIsPropagated() {
		@Configuration class Config {
			@Lazy @Bean
			Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).isLazyInit()).as("lazy metadata was not propagated").isTrue();
	}

	@Test
	void lazyMetadataIsFalseByDefault() {
		@Configuration class Config {
			@Bean Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).isLazyInit()).as("@Bean methods should be non-lazy by default").isFalse();
	}

	@Test
	void defaultLazyConfigurationPropagatesToIndividualBeans() {
		@Lazy @Configuration class Config {
			@Bean Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).isLazyInit()).as("@Bean methods declared in a @Lazy @Configuration should be lazily instantiated").isTrue();
	}

	@Test
	void eagerBeanOverridesDefaultLazyConfiguration() {
		@Lazy @Configuration class Config {
			@Lazy(false) @Bean Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).isLazyInit()).as("@Lazy(false) @Bean methods declared in a @Lazy @Configuration should be eagerly instantiated").isFalse();
	}

	@Test
	void eagerConfigurationProducesEagerBeanDefinitions() {
		@Lazy(false) @Configuration class Config {  // will probably never happen, doesn't make much sense
			@Bean Object foo() { return null; }
		}

		assertThat(beanDef(Config.class).isLazyInit()).as("@Lazy(false) @Configuration should produce eager bean definitions").isFalse();
	}

	private AbstractBeanDefinition beanDef(Class<?> configClass) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("config", new RootBeanDefinition(configClass));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(factory);
		return (AbstractBeanDefinition) factory.getBeanDefinition("foo");
	}

}
