/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class DuplicateConfigurationClassPostProcessorTests {

	@Test
	public void repro() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition("a", new RootBeanDefinition(ConfigurationClassPostProcessor.class));
		ctx.registerBeanDefinition("b", new RootBeanDefinition(ConfigurationClassPostProcessor.class));
		ctx.registerBeanDefinition("myConfig", new RootBeanDefinition(Config.class));
		ctx.refresh();
	}

	@Configuration
	static class Config {
		@Bean
		public String string() {
			return "bean";
		}
	}
}
