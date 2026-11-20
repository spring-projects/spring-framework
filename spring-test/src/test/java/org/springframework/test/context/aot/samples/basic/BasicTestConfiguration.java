/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.basic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.aot.samples.common.DefaultMessageService;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.aot.samples.common.SpanishMessageService;
import org.springframework.test.context.aot.samples.management.Managed;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@Configuration(proxyBeanMethods = false)
class BasicTestConfiguration {

	@Bean
	@Profile("default")
	@Managed
	MessageService defaultMessageService() {
		return new DefaultMessageService();
	}

	@Bean
	@Profile("spanish")
	@Managed
	MessageService spanishMessageService() {
		return new SpanishMessageService();
	}

	@Bean
	Number magicNumber() {
		return 42;
	}

}
