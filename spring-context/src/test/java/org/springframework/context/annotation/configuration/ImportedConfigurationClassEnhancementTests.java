/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests cornering the bug exposed in SPR-6779.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class ImportedConfigurationClassEnhancementTests {

	@Test
	void autowiredConfigClassIsEnhancedWhenImported() {
		autowiredConfigClassIsEnhanced(ConfigThatDoesImport.class);
	}

	@Test
	void autowiredConfigClassIsEnhancedWhenRegisteredViaConstructor() {
		autowiredConfigClassIsEnhanced(ConfigThatDoesNotImport.class, ConfigToBeAutowired.class);
	}

	@SuppressWarnings("deprecation")
	private void autowiredConfigClassIsEnhanced(Class<?>... configClasses) {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(configClasses);
		Config config = ctx.getBean(Config.class);
		assertThat(ClassUtils.isCglibProxy(config.autowiredConfig)).as("autowired config class has not been enhanced").isTrue();
		ctx.close();
	}


	@Test
	void autowiredConfigClassBeanMethodsRespectScopingWhenImported() {
		autowiredConfigClassBeanMethodsRespectScoping(ConfigThatDoesImport.class);
	}

	@Test
	void autowiredConfigClassBeanMethodsRespectScopingWhenRegisteredViaConstructor() {
		autowiredConfigClassBeanMethodsRespectScoping(ConfigThatDoesNotImport.class, ConfigToBeAutowired.class);
	}

	private void autowiredConfigClassBeanMethodsRespectScoping(Class<?>... configClasses) {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(configClasses);
		Config config = ctx.getBean(Config.class);
		TestBean testBean1 = config.autowiredConfig.testBean();
		TestBean testBean2 = config.autowiredConfig.testBean();
		assertThat(testBean1)
				.as("got two distinct instances of testBean when singleton scoping was expected")
				.isSameAs(testBean2);
		ctx.close();
	}


	@Test
	void importingNonConfigurationClassCausesBeanDefinitionParsingException() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigThatImportsNonConfigClass.class);
		ConfigThatImportsNonConfigClass config = ctx.getBean(ConfigThatImportsNonConfigClass.class);
		assertThat(config.testBean).isSameAs(ctx.getBean(TestBean.class));
		ctx.close();
	}



	@Configuration
	static class ConfigToBeAutowired {

		@Bean TestBean testBean() {
			return new TestBean();
		}
	}

	static class Config {

		@Autowired ConfigToBeAutowired autowiredConfig;
	}

	@Import(ConfigToBeAutowired.class)
	@Configuration
	static class ConfigThatDoesImport extends Config {
	}

	@Configuration
	static class ConfigThatDoesNotImport extends Config {
	}

	@Configuration
	@Import(TestBean.class)
	static class ConfigThatImportsNonConfigClass {

		@Autowired TestBean testBean;
	}

}
