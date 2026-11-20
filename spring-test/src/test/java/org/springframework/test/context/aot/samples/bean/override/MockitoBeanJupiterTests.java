/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.bean.override;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.aot.samples.common.GreetingService;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
public class MockitoBeanJupiterTests {

	/**
	 * Mock for nonexistent bean.
	 */
	@MockitoBean
	GreetingService greetingService;

	/**
	 * Mock for existing bean.
	 */
	@MockitoBean
	MessageService messageService;

	@BeforeEach
	void configureMocks(@Autowired GreetingService greetingService, @Autowired MessageService messageService) {
		when(greetingService.greeting()).thenReturn("enigma");
		when(messageService.generateMessage()).thenReturn("override");
	}

	@Test
	void test() {
		assertThat(greetingService.greeting()).isEqualTo("enigma");
		assertThat(messageService.generateMessage()).isEqualTo("override");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		MessageService messageService() {
			return () -> "prod";
		}
	}

}
