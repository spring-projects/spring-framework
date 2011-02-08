/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.configuration.StubSpecification;
import org.springframework.context.config.FeatureSpecification;

/**
 * Tests proving that @Configuration classes may @Import @FeatureConfiguration
 * classes, and vice versa.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class FeatureConfigurationImportTests {

	@Test
	public void importFeatureConfigurationFromConfiguration() {
		ConfigurableApplicationContext ctx =
			new AnnotationConfigApplicationContext(ImportingConfig.class);
		ImportedFeatureConfig ifc = ctx.getBean(ImportedFeatureConfig.class);
		assertThat(
				"@FeatureConfiguration class was imported and registered " +
				"as a bean but its @Feature method was never called",
				ifc.featureMethodWasCalled, is(true));
	}

	@Test
	public void importConfigurationFromFeatureConfiguration() {
		ConfigurableApplicationContext ctx =
			new AnnotationConfigApplicationContext(ImportingFeatureConfig.class);
		ImportingFeatureConfig ifc = ctx.getBean(ImportingFeatureConfig.class);
		ImportedConfig ic = ctx.getBean(ImportedConfig.class);
		assertThat(
				"@FeatureConfiguration class was registered directly against " +
				"the container but its @Feature method was never called",
				ifc.featureMethodWasCalled, is(true));
		assertThat(
				"@Configuration class was @Imported but its @Bean method" +
				"was never registered / called",
				ic.beanMethodWasCalled, is(true));
	}


	@Configuration
	@Import(ImportedFeatureConfig.class)
	static class ImportingConfig {
	}


	@FeatureConfiguration
	static class ImportedFeatureConfig {
		boolean featureMethodWasCalled = false;

		@Feature
		public FeatureSpecification f() {
			this.featureMethodWasCalled = true;
			return new StubSpecification();
		}
	}


	@Configuration
	static class ImportedConfig {
		boolean beanMethodWasCalled = true;
		@Bean
		public TestBean testBean() {
			this.beanMethodWasCalled = true;
			return new TestBean();
		}
	}


	@FeatureConfiguration
	@Import(ImportedConfig.class)
	static class ImportingFeatureConfig {
		boolean featureMethodWasCalled = false;

		@Feature
		public FeatureSpecification f() {
			this.featureMethodWasCalled = true;
			return new StubSpecification();
		}
	}

}
