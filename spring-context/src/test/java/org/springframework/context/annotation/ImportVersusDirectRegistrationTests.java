/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Andy Wilkinson
 */
class ImportVersusDirectRegistrationTests {

	@Test
	void thingIsNotAvailableWhenOuterConfigurationIsRegisteredDirectly() {
		try (AnnotationConfigApplicationContext directRegistration = new AnnotationConfigApplicationContext()) {
			directRegistration.register(AccidentalLiteConfiguration.class);
			directRegistration.refresh();
			assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
					directRegistration.getBean(Thing.class));
		}
	}

	@Test
	void thingIsNotAvailableWhenOuterConfigurationIsRegisteredWithClassName() {
		try (AnnotationConfigApplicationContext directRegistration = new AnnotationConfigApplicationContext()) {
			directRegistration.registerBeanDefinition("config",
					new RootBeanDefinition(AccidentalLiteConfiguration.class.getName()));
			directRegistration.refresh();
			assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
					directRegistration.getBean(Thing.class));
		}
	}

	@Test
	void thingIsNotAvailableWhenOuterConfigurationIsImported() {
		try (AnnotationConfigApplicationContext viaImport = new AnnotationConfigApplicationContext()) {
			viaImport.register(Importer.class);
			viaImport.refresh();
			assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
					viaImport.getBean(Thing.class));
		}
	}

}


@Import(AccidentalLiteConfiguration.class)
class Importer {
}


class AccidentalLiteConfiguration {

	@Configuration
	class InnerConfiguration {

		@Bean
		public Thing thing() {
			return new Thing();
		}
	}
}


class Thing {
}
