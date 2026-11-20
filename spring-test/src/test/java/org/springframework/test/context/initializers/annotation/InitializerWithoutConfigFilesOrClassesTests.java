/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.initializers.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.initializers.annotation.InitializerWithoutConfigFilesOrClassesTests.EntireAppInitializer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies support for {@link ApplicationContextInitializer
 * ApplicationContextInitializers} in the TestContext framework when the test class
 * declares neither XML configuration files nor annotated configuration classes.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitConfig(initializers = EntireAppInitializer.class)
class InitializerWithoutConfigFilesOrClassesTests {

	@Autowired
	private String foo;


	@Test
	void foo() {
		assertThat(foo).isEqualTo("foo");
	}


	static class EntireAppInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			new AnnotatedBeanDefinitionReader(applicationContext).register(GlobalConfig.class);
		}
	}

}
