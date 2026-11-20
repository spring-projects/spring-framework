/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.basic;

import org.junit.jupiter.api.Test;

import org.springframework.aot.AotDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.aot.samples.common.DefaultMessageService;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DisabledInAotMode} method-level tests.
 *
 * <p>The {@code ApplicationContext} will still be processed for AOT optimizations
 * and used for the {@link #test} method (in standard JVM mode and in AOT mode),
 * but the {@link #disabledInAotMode()} method will not be executed in AOT mode.
 *
 * @author Sam Brannen
 * @since 6.1
 * @see DisabledInAotRuntimeClassLevelTests
 * @see DisabledInAotProcessingTests
 */
@SpringJUnitConfig
@TestPropertySource(properties = "disabledInAotMode = method-level")
public class DisabledInAotRuntimeMethodLevelTests {

	@Test
	void test(@Autowired MessageService messageService,
			@Value("${disabledInAotMode}") String disabledInAotMode) {

		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		assertThat(disabledInAotMode).isEqualTo("method-level");
	}

	@Test
	@DisabledInAotMode
	void disabledInAotMode() {
		assertThat(AotDetector.useGeneratedArtifacts()).as("Should be disabled in AOT mode").isFalse();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		MessageService defaultMessageService() {
			return new DefaultMessageService();
		}
	}

}
