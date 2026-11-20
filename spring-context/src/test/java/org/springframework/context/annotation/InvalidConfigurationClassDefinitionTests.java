/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * Unit tests covering cases where a user defines an invalid Configuration
 * class, for example: forgets to annotate with {@link Configuration} or declares
 * a Configuration class as final.
 *
 * @author Chris Beams
 */
class InvalidConfigurationClassDefinitionTests {

	@Test
	void configurationClassesMayNotBeFinal() {
		@Configuration
		final class Config {
			@Bean String dummy() { return "dummy"; }
		}

		BeanDefinition configBeanDef = rootBeanDefinition(Config.class).getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", configBeanDef);

		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> pp.postProcessBeanFactory(beanFactory))
				.withMessageContaining("Remove the final modifier");
	}

}
