/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBeanManuallyRegisteredSingletonTests.SingletonRegistrar;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies support for overriding a manually registered singleton bean with
 * {@link MockitoBean @MockitoBean}.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig(initializers = SingletonRegistrar.class)
class MockitoBeanManuallyRegisteredSingletonTests {

	@MockitoBean
	MessageService messageService;

	@Test
	void test() {
		when(messageService.getMessage()).thenReturn("override");
		assertThat(messageService.getMessage()).isEqualTo("override");
	}

	static class SingletonRegistrar implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getBeanFactory().registerSingleton("messageService", new MessageService());
		}
	}

	static class MessageService {

		String getMessage() {
			return "production";
		}
	}

}
