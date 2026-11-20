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
import org.springframework.test.context.bean.override.easymock.EasyMockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

/**
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
public class EasyMockBeanJupiterTests {

	/**
	 * Mock for nonexistent bean.
	 */
	@EasyMockBean
	GreetingService greetingService;

	/**
	 * Mock for existing bean.
	 */
	@EasyMockBean
	MessageService messageService;

	@BeforeEach
	void configureMocks(@Autowired GreetingService greetingService, @Autowired MessageService messageService) {
		expect(greetingService.greeting()).andReturn("enigma");
		expect(messageService.generateMessage()).andReturn("override");
		replay(greetingService, messageService);
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
