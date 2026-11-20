/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Corners the bug originally reported by SPR-8824, where the presence of two
 * {@link ConfigurationClassPostProcessor} beans in combination with a @Configuration
 * class having at least one @Bean method causes a "Singleton 'foo' isn't currently in
 * creation" exception.
 *
 * @author Chris Beams
 * @since 3.1
 */
class DuplicateConfigurationClassPostProcessorTests {

	@Test
	void repro() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition("a", new RootBeanDefinition(ConfigurationClassPostProcessor.class));
		ctx.registerBeanDefinition("b", new RootBeanDefinition(ConfigurationClassPostProcessor.class));
		ctx.registerBeanDefinition("myConfig", new RootBeanDefinition(Config.class));
		ctx.refresh();
		ctx.close();
	}

	@Configuration
	static class Config {
		@Bean
		String string() {
			return "bean";
		}
	}

}
